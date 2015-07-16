package osgi.enroute.iot.gpio.util;

import osgi.enroute.dto.api.DTOs;
import osgi.enroute.iot.gpio.api.CircuitBoard;


/**
 * IC that acts as a general purpose output of the real hardware. Calling the
 * input method will set an actual hardware output.
 */
public abstract class GPO extends ICAdapter<Digital, Void> implements Digital {

	public GPO(String name, CircuitBoard board, DTOs dtos) {
		super(name,dtos,board);
	}
	
}
