package osgi.enroute.iot.gpio.api;

import osgi.enroute.iot.admin.dto.ICDTO;

/**
 * Defines a software component that reacts to incoming signals and creates
 * outgoing signals.
 */
public interface IC {

	/**
	 * Get the IC's descriptor
	 * 
	 * @return The descriptor of the IC
	 */
	ICDTO getDTO();

	/**
	 * Set a new value on the IC's pin
	 * 
	 * @param pin
	 *            the input pin to set the value
	 * @param value
	 *            the actual value
	 * @throws Exception
	 */
	void fire(String pin, Object value) throws Exception;

	/**
	 * Get the name of the device
	 * 
	 * @return the name of the device
	 */
	default String getName() {
		return null;
	}
}
