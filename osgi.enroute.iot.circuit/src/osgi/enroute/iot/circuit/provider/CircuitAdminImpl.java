package osgi.enroute.iot.circuit.provider;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.lib.collections.MultiMap;
import osgi.enroute.dto.api.DTOs;
import osgi.enroute.iot.admin.api.CircuitAdmin;
import osgi.enroute.iot.admin.api.IotAdminConstants;
import osgi.enroute.iot.admin.dto.Delta;
import osgi.enroute.iot.admin.dto.ICDTO;
import osgi.enroute.iot.admin.dto.WireDTO;
import osgi.enroute.iot.circuit.provider.ICTracker.OutputPin;
import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.scheduler.api.Scheduler;

/**
 * Implementation of {@link CircuitAdmin}
 */

@ProvideCapability(ns = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, name = IotAdminConstants.IOT_ADMIN_SPECIFICATION_NAME, version = IotAdminConstants.IOT_ADMIN_SPECIFICATION_VERSION)
@Component(immediate = true, name = "osgi.enroute.iot.circuit")
public class CircuitAdminImpl implements CircuitAdmin, CircuitBoard {
	private static final String			WIREFACTORYPID	= "osgi.enroute.iot.circuit.wires";
	final Map<IC, ICTracker>			ics				= new IdentityHashMap<>();
	final MultiMap<String, ICTracker>	index			= new MultiMap<>();
	final Map<String, WireImpl>			wires			= new ConcurrentHashMap<>();
	final AtomicInteger					id				= new AtomicInteger(
			1000);
	final Object						lock			= new Object();;

	DTOs												dtos;
	ServiceTracker<IC, ICTracker>						tracker;
	EventAdmin											ea;
	private ConfigurationAdmin							cm;
	private ServiceRegistration<ManagedServiceFactory>	msf;
	private Set<String>									names	= new HashSet<>();
	private Scheduler									sc;

	/*
	 * Activate the circuit board. We first read the board and then allow the
	 */

	@Activate
	void activate(Map<String, Object> map, BundleContext context)
			throws Exception {
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put(Constants.SERVICE_PID, WIREFACTORYPID);
		msf = context.registerService(ManagedServiceFactory.class,
				new ManagedServiceFactory() {

					@Override
					public void updated(String pid,
							Dictionary<String, ?> properties)
									throws ConfigurationException {
						try {
							Map<String, Object> map = new HashMap<>();
							Enumeration<String> keys = properties.keys();
							while (keys.hasMoreElements()) {
								String key = keys.nextElement();
								Object value = properties.get(key);
								map.put(key, value);
							}

							WireImpl wire = dtos.convert(map)
									.to(WireImpl.class);
							wire.circuit = CircuitAdminImpl.this;
							addWire(wire, pid);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					@Override
					public String getName() {
						return WIREFACTORYPID;
					}

					@Override
					public void deleted(String pid) {
						WireImpl wire = wires.get(pid);
						if (wire == null)
							return;

						removeWire(wire, pid);
					}
				}, props);

		//
		// To break any circularity
		//

		sc.after(() -> {
			tracker = getTracker(context);
			tracker.open();
		} , 200);
	}

	@Deactivate
	void deactivate(Map<String, Object> map) {
		msf.unregister();
		tracker.close();
	}

	/**
	 * Create a new wire.
	 *
	 * TODO check if wire already exists
	 */
	@Override
	public WireDTO connect(String fromDevice, String fromPin, String toDevice,
			String toPin) throws Exception {

		WireDTO wire = new WireDTO();
		wire.wireId = id.incrementAndGet();
		wire.fromDevice = fromDevice;
		wire.fromPin = fromPin;
		wire.toDevice = toDevice;
		wire.toPin = toPin;
		wire.wired = false;

		Hashtable<String, Object> ht = new Hashtable<>(dtos.asMap(wire));

		Configuration config = cm.createFactoryConfiguration(WIREFACTORYPID,
				"?");
		config.update(ht);
		return wire;
	}

	/**
	 * Remove an existing wire
	 */
	@Override
	public boolean disconnect(int wireId) throws Exception {
		Configuration[] list = cm.listConfigurations("(wireId=" + wireId + ")");
		if (list == null || list.length == 0)
			return false;

		list[0].delete();
		return true;
	}

	/**
	 * Return a list of wires. The list is owned by the caller.
	 */
	@Override
	public List<WireDTO> getWires() {
		return new ArrayList<>(wires.values());
	}

	@Override
	public ICDTO[] getICs() {
		return ics.values().stream().map((ICTracker t) -> t.icdto)
				.toArray((n) -> new ICDTO[n]);
	}

	/*
	 * Add a new IC to the circuit and wire it up if necessary
	 */
	void addTracker(ICTracker tracker) {
		synchronized (lock) {

			ics.put(tracker.ic, tracker);
			index.add(tracker.icdto.deviceId, tracker);

			for (WireImpl wire : wires.values()) {
				if (!wire.wired) {
					if (wire.fromDevice.equals(tracker.icdto.deviceId)
							|| wire.toDevice.equals(tracker.icdto.deviceId))
						wire.connect();
				}
			}
		}
		event();
	}

	private void event() {
		Map<String, Object> props = new HashMap<String, Object>();
		Delta delta = new Delta();
		delta.time = System.currentTimeMillis();

		// TODO Add the data

		Event e = new Event(CircuitAdmin.TOPIC, props);
		ea.postEvent(e);
	}

	/*
	 * Remove an IC from the circuit, breaking any wires if necessary
	 */
	boolean removeTracker(ICTracker tracker) {
		boolean event = false;
		boolean result;
		synchronized (lock) {
			for (WireImpl wire : wires.values()) {
				if (wire.wired) {
					if (wire.input == null || wire.output == null) {
						System.err.println("Oops??");
					}
					if (wire.input.getTracker() == tracker
							|| wire.output.getTracker() == tracker)
						wire.disconnect();
					event = true;
				}
			}
			result = ics.remove(tracker.ic, tracker);
			index.remove(tracker.icdto.deviceId, tracker);
		}
		if (event)
			event();

		return result;
	}

	/*
	 * Add a new wire
	 */
	void addWire(WireImpl wire, String pid) {
		synchronized (lock) {
			wires.put(pid, wire);
			wire.connect();
		}
		event();
	}

	/*
	 * Remove an existing wire
	 */
	boolean removeWire(WireImpl wire, String pid) {
		boolean result;
		synchronized (lock) {
			result = wires.remove(pid) != null;
			wire.disconnect();
		}
		event();
		return result;
	}

	/*
	 * Create a tracker on the ICs. Each IC will be added to the circuit and
	 * wired if necessary
	 */
	private ServiceTracker<IC, ICTracker> getTracker(BundleContext context) {
		return new ServiceTracker<IC, ICTracker>(context, IC.class, null) {
			@Override
			public ICTracker addingService(ServiceReference<IC> reference) {
				try {
					IC ic = context.getService(reference);
					if (ic == null)
						return null;

					String name = getName(reference, ic);

					ICTracker tracker = new ICTracker(name, ic,
							CircuitAdminImpl.this);
					addTracker(tracker);
					return tracker;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
			}

			/*
			 * Get the name of the IC. We first try the 'name' service property
			 * because that is set by config admin. Then we try the getname
			 * method on the IC, then the PID, and last resort is service id.
			 */
			private String getName(ServiceReference<IC> reference, IC ic) {

				String name = (String) reference.getProperty("name");

				if (name == null || name.isEmpty()) {
					name = ic.getName();
					if (name == null) {

						name = (String) reference
								.getProperty(Constants.SERVICE_PID);
						if (name == null) {

							name = reference.getProperty(Constants.SERVICE_ID)
									+ "";
						}
					}
				}

				synchronized (names) {
					int n = 1;
					String t = name;
					while (names.contains(t))
						t = name + "-" + n++;

					name = t;
					names.add(name);
				}
				return name;
			}

			@Override
			public void removedService(ServiceReference<IC> reference,
					ICTracker service) {
				try {
					synchronized (names) {
						names.remove(service.getName());
					}
					super.removedService(reference, service);
					removeTracker(service);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
	}

	@Override
	public boolean fire(IC ic, String pin, Object value) {

		ICTracker tracker;

		synchronized (ics) {
			tracker = ics.get(ic);
		}

		if (tracker != null) {
			OutputPin outputPin = tracker.getOutputPin(pin);
			if (outputPin != null) {
				outputPin.fire(value);
				return true;
			}
		}
		return false;
	}

	ICTracker getIC(String toDevice) {
		List<ICTracker> list = index.get(toDevice);
		if (list == null || list.isEmpty())
			return null;

		return list.get(0);
	}

	@Reference
	void setDtos(DTOs dtos) {
		this.dtos = dtos;
	}

	@Reference
	void setCm(ConfigurationAdmin cm) {
		this.cm = cm;
	}

	@Reference
	void setEventAdmin(EventAdmin ea) {
		this.ea = ea;
	}

	@Reference
	void setScheduler(Scheduler sc) {
		this.sc = sc;
	}
}
