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
		Debug.COMMAND_FUNCTION + "=pid" //
}, name = "osgi.enroute.iot.circuit.command")
public class CircuitCommand {
	CircuitAdmin						ca;
	private BundleContext				context;
	Map<String, ServiceRegistration<?>>	regs	= new ConcurrentHashMap<>();
	private CircuitBoard	board;
	private DTOs	dtos;

	@Activate
	void activate(BundleContext context) {
		this.context = context;
	}

	public String circuit() {
		return "help";
	}

	public List<WireDTO> wires() {
		return ca.getWires();
	}

	public ICDTO[] ics() {
		return ca.getICs();
	}

	public WireDTO connect(String fromDevice, String fromPin, String toDevice,
			String toPin) throws Exception {
		fromDevice = pid(fromDevice);
		toDevice = pid(toDevice);
		return ca.connect(fromDevice, fromPin, toDevice, toPin);
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

	public boolean disconnect(int wireId) throws Exception {
		return ca.disconnect(wireId);
	}

	class SysOut extends GPO {
		public SysOut(String name, CircuitBoard board, DTOs dtos) {
			super(name, board, dtos);
		}

		@Override
		public void set(boolean value) throws Exception {
			System.out.println("Setting " + value);
		}
	}

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

	class Clock extends GPI implements CronJob<Object> {
		boolean	value;

		public Clock(String name, CircuitBoard board, DTOs dtos) {
			super(name, board, dtos);
		}
		@Override
		public void run(Object data) throws Exception {
			out().set(value = !value);
		}
	};

	public void clock(String name) {
		Clock clock = new Clock(name, board,dtos);
		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(Constants.SERVICE_PID, name);
		properties.put(CronJob.CRON, "/5 * * * * ?");
		ServiceRegistration<?> reg = context.registerService(new String[] {
				IC.class.getName(), CronJob.class.getName() }, clock,
				properties);
		ServiceRegistration<?> old = regs.put(name, reg);
		if (old != null)
			old.unregister();
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
