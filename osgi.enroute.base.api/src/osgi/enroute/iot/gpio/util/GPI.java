package osgi.enroute.iot.gpio.util;

import osgi.enroute.dto.api.DTOs;
import osgi.enroute.iot.gpio.api.CircuitBoard;


/**
 * An IC that acts as a General Purpose Input from the real hardware. If the
 * hardware changes, the output is signalled.
 */
public class GPI extends ICAdapter<Void, Digital>{

	/**
	 * A General purpose input pin.
	 * 
	 * @param name
	 *            the name of the pin
	 * @param board
	 *            the connected circuit board
	 * @param dtos
	 *            TODO
	 */
	public GPI(String name, CircuitBoard board, DTOs dtos) {
		super(name,dtos,board);
	}

}
