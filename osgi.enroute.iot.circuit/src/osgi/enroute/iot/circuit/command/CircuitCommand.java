package osgi.enroute.iot.circuit.command;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import osgi.enroute.debug.api.Debug;
import osgi.enroute.dto.api.DTOs;
import osgi.enroute.iot.admin.api.CircuitAdmin;
import osgi.enroute.iot.admin.dto.ICDTO;
import osgi.enroute.iot.admin.dto.WireDTO;
import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.util.GPI;
import osgi.enroute.iot.gpio.util.GPO;
import osgi.enroute.scheduler.api.CronJob;
import osgi.enroute.scheduler.api.Scheduler;

/**
 * Commands to work with a circuit
 * 
 */
@Component(service = CircuitCommand.class, property = {
		Debug.COMMAND_SCOPE + "=circuit", //
		Debug.COMMAND_FUNCTION + "=circuit", //
		Debug.COMMAND_FUNCTION + "=wires", //
		Debug.COMMAND_FUNCTION + "=ics", //
		Debug.COMMAND_FUNCTION + "=connect", //
		Debug.COMMAND_FUNCTION + "=gpo", //
		Debug.COMMAND_FUNCTION + "=clock", //
		Debug.COMMAND_FUNCTION + "=disconnect", //
}, name = "osgi.enroute.iot.circuit.command")
public class CircuitCommand {
	CircuitAdmin						ca;
	private BundleContext				context;
	Map<String, ServiceRegistration<?>>	regs	= new ConcurrentHashMap<>();
	private CircuitBoard				board;
	private DTOs						dtos;

	@Activate
	void activate(BundleContext context) {
		this.context = context;
	}

	public String circuit() {
		return //
		"wires                           – Show the existing wires\n"
				+ "ics                             – Show the current ICs\n"
				+ "connect <from> <pin> <to> <pin> – Connect two ics\n"
				+ "gpo <id>                        – Create a test output to the Console\n"
				+ "clock <id>                      – Create a test clock\n"
				+ "disconnect id                   – Disconnect a write\n"
				+ "\n";
	}

	/**
	 * List the wires
	 */
	public List<WireDTO> wires() {
		return ca.getWires();
	}

	/**
	 * List the ICs
	 */
	public ICDTO[] ics() {
		return ca.getICs();
	}

	/**
	 * Connect 2 ICs from an input pin to an output pin
	 * 
	 * @param fromDevice
	 *            The IC's id that is the source
	 * @param fromPin
	 *            the source's pin
	 * @param toDevice
	 *            the IC's id that is the destination
	 * @param toPin
	 *            the destination's pin
	 * @return A description of the wire
	 */
	public WireDTO connect(String fromDevice, String fromPin, String toDevice,
			String toPin) throws Exception {
		fromDevice = pid(fromDevice);
		toDevice = pid(toDevice);
		return ca.connect(fromDevice, fromPin, toDevice, toPin);
	}

	/**
	 * Disconnect a wire by its ide
	 * 
	 * @param wireId
	 *            the wire's id
	 * @return if the wire existed
	 */
	public boolean disconnect(int wireId) throws Exception {
		return ca.disconnect(wireId);
	}

	/**
	 * The class that implements a GPO by sending any input to the System.out
	 */
	class SysOut extends GPO {
		public SysOut(String name, CircuitBoard board, DTOs dtos) {
			super(name, board, dtos);
		}

		@Override
		public void set(boolean value) throws Exception {
			System.out.println("Setting " + value);
		}
	}

	/**
	 * Create an IC that sends any input to System.out. The life cycle of this
	 * IC is depending on this component, so it is not persistent.
	 * 
	 * @param name the name/id of this IC
	 */
	public void gpo(String name) {
		SysOut gpo = new SysOut(name, board, dtos);
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(Constants.SERVICE_PID, name);
		ServiceRegistration<?> reg = context.registerService(IC.class, gpo,
				properties);
		ServiceRegistration<?> old = regs.put(name, reg);
		if (old != null)
			old.unregister();
	}

	/**
	 * Create an IC that reverses the output every second. 
	 */
	class Clock extends GPI implements CronJob<Object> {
		boolean	value;

		public Clock(String name, CircuitBoard board, DTOs dtos) {
			super(name, board, dtos);
		}

		@Override
		public void run(Object data) throws Exception {
			out().set(value = !value);
		}
	}
	
	/**
	 * Create a clock that reverses its output pin ever second. The life cycle of this
	 * IC is depending on this component, so it is not persistent.
	 * 
	 * @param name The name/id of this IC
	 */

	public void clock(String name) {
		Clock clock = new Clock(name, board, dtos);
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(Constants.SERVICE_PID, name);
		properties.put(CronJob.CRON, "/1 * * * * ?");
		ServiceRegistration<?> reg = context.registerService(new String[] {
				IC.class.getName(), CronJob.class.getName() }, clock,
				properties);
		ServiceRegistration<?> old = regs.put(name, reg);
		if (old != null)
			old.unregister();
	}


	private String pid(String pid) throws InvalidSyntaxException {
		try {
			long serviceid = Long.parseLong(pid);
			ServiceReference<?>[] ref = context.getServiceReferences(
					(String) null, "(service.id=" + serviceid + ")");
			if (ref == null || ref.length == 0) {
				String p2 = (String) ref[0].getProperty(Constants.SERVICE_PID);
				if (p2 != null)
					return p2;
			}
		} catch (NumberFormatException e) {

		}
		return pid;
	}

	@Reference
	void setCircuitAdmin(CircuitAdmin ca) {
		this.ca = ca;
	}

	@Reference
	void setCircuitBoard(CircuitBoard board) {
		this.board = board;
	}

	@Reference
	void setDTOs(DTOs dtos) {
		this.dtos = dtos;
	}

	@Reference
	void setScheduler(Scheduler scheduler) {
	}
}
