package osgi.enroute.iot.gpio.util;

/**
 * A enable pin
 */
public interface Enable {
	/**
	 * The value of the pin
	 * 
	 * @param enable
	 *            the value of the pin
	 * @throws Exception
	 */
	void enbl(boolean enable) throws Exception;
}
