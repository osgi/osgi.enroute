package osgi.enroute.iot.gpio.util;

/**
 * A 1-pin input/output
 */
public interface Digital {
	/**
	 * Set the value of the pin
	 * 
	 * @param value
	 *            the value
	 * @throws Exception
	 */
	void set(boolean value) throws Exception;
}
