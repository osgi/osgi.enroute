package osgi.enroute.iot.gpio.util;

/**
 * An analog pin
 */
public interface Analog {

	/**
	 * Set the single pin analog value
	 * 
	 * @param value
	 *            the given value
	 */
	void set(double value);
}
