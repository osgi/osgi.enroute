package osgi.enroute.iot.circuit.provider;

import java.util.Map;

import osgi.enroute.iot.admin.dto.WireDTO;
import osgi.enroute.iot.circuit.provider.ICTracker.InputPin;
import osgi.enroute.iot.circuit.provider.ICTracker.OutputPin;

/**
 * Represents the wire for us. Note that this is a DTO and is read from disk. So
 * this class must remain public and have a no-arg constructor.
 * 
 */
public class WireImpl extends WireDTO {
	OutputPin			output;
	InputPin			input;
	CircuitAdminImpl	circuit;
	public Map<String,Object> __extra;
	public String factoryPid;
	

	boolean connect() {
		ICTracker from = circuit.getIC(fromDevice);
		if (from == null)
			return false;

		ICTracker to = circuit.getIC(toDevice);
		if (to == null)
			return false;

		output = from.getOutputPin(fromPin);
		if (output == null) {
			// TODO log
			return false;
		}

		input = to.getInputPin(toPin);
		if (input == null) {
			// TODO log
			return false;
		}

		output.subscribe(input);
		wired = true;
		try {
			input.fire(output.lastValue);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	boolean disconnect() {
		if (wired) {
			wired = false;
			return output.unsubscribe(input);
		}
		return false;
	}
}
