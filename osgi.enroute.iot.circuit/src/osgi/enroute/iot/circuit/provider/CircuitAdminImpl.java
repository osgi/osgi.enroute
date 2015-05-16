package osgi.enroute.iot.circuit.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;

import osgi.enroute.dto.api.DTOs;
import osgi.enroute.iot.admin.api.CircuitAdmin;
import osgi.enroute.iot.admin.dto.Delta;
import osgi.enroute.iot.admin.dto.ICDTO;
import osgi.enroute.iot.admin.dto.WireDTO;
import osgi.enroute.iot.circuit.provider.ICTracker.OutputPin;
import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;
import aQute.lib.collections.MultiMap;

/**
 * Implementation of {@link CircuitAdmin}
 */
@Component(immediate = true, name = "osgi.enroute.iot.circuit")
public class CircuitAdminImpl implements CircuitAdmin, CircuitBoard {

	final Map<IC, ICTracker>		ics		= new IdentityHashMap<>();
	final MultiMap<String, ICTracker>	index	= new MultiMap<>();
	final Map<Integer, WireImpl>	wires	= new ConcurrentHashMap<>();
	final AtomicInteger				id		= new AtomicInteger(1000);
	final Object					lock	= new Object();					;

	File							schema;
	DTOs							dtos;
	ServiceTracker<IC, ICTracker>	tracker;
	EventAdmin						ea;

	@Activate
	void activate(Map<String, Object> map, BundleContext context)
			throws Exception {
		schema = context.getDataFile("circuit.schema");
		readCircuit(schema);

		tracker = getTracker(context);
		tracker.open();
	}

	@Deactivate
	void deactivate(Map<String, Object> map) {
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
		WireImpl wire = new WireImpl();
		wire.circuit = this;
		wire.wireId = id.incrementAndGet();
		wire.fromDevice = fromDevice;
		wire.fromPin = fromPin;
		wire.toDevice = toDevice;
		wire.toPin = toPin;
		wire.wired = false;
		addWire(wire);
		save();
		return wire;
	}

	/**
	 * Remove an existing wire
	 */
	@Override
	public boolean disconnect(int wireId) throws Exception {
		WireImpl wire = wires.get(wireId);
		if (wire == null)
			return false;

		if (removeWire(wire)) {
			save();
			return true;
		}
		return false;
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
	 * Save the current circuit
	 */
	private void save() throws Exception {
		synchronized (lock) {
			try (FileOutputStream fout = new FileOutputStream(schema)) {
				CircuitDTO c = new CircuitDTO();
				c.wires = new ArrayList<>(wires.values());
				dtos.encoder(c.wires).put(fout);
			}
		}
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
	void addWire(WireImpl wire) {
		synchronized (lock) {
			wires.put(wire.wireId, wire);
			wire.connect();
		}
		event();
	}

	/*
	 * Remove an existing wire
	 */
	boolean removeWire(WireImpl wire) {
		boolean result;
		synchronized (lock) {
			result = wires.remove(wire.wireId) != null;
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

					String pid = (String) reference.getProperty("name");
					if (pid == null) {
						pid = (String) reference
								.getProperty(Constants.SERVICE_PID);
						if (pid == null) {
							pid = reference.getProperty(Constants.SERVICE_ID)
									+ "";
						}
					}

					ICTracker tracker = new ICTracker(pid, ic,
							CircuitAdminImpl.this);
					addTracker(tracker);
					return tracker;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
			}

			@Override
			public void removedService(ServiceReference<IC> reference,
					ICTracker service) {
				try {
					super.removedService(reference, service);
					removeTracker(service);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
	}

	/*
	 * Read the circuit from disk
	 */
	private void readCircuit(File schema) {
		if (schema.isFile())
			try {
				FileInputStream in = new FileInputStream(schema);
				CircuitDTO circuit = dtos.decoder(CircuitDTO.class).get(in);
				if (circuit.wires != null) {
					for (WireImpl wire : circuit.wires) {
						wire.circuit = this;
						wire.wireId = id.incrementAndGet();
						wires.put(wire.wireId, wire);
					}
				}
			} catch (Exception e) {
				// TODO
				// System.err.println("Cannot read circuit " + e);
			}
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
		if ( list == null || list.isEmpty())
			return null;
		
		return list.get(0);
	}

	@Reference
	void setDtos(DTOs dtos) {
		this.dtos = dtos;
	}

	@Reference
	void setEventAdmin(EventAdmin ea) {
		this.ea = ea;
	}

}
