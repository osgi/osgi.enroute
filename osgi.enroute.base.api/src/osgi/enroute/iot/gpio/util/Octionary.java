package osgi.enroute.iot.gpio.util;

/**
 * An 8-bit input/output type
 */
public interface Octionary {
	/**
	 * bit 0
	 * 
	 * @param value
	 *            the value
	 */
	void d0(boolean value);

	/**
	 * bit 1
	 * 
	 * @param value
	 *            the value
	 */
	void d1(boolean value);

	/**
	 * bit 2
	 * 
	 * @param value
	 *            the value
	 */
	void d2(boolean value);

	/**
	 * bit 3
	 * 
	 * @param value
	 *            the value
	 */
	void d3(boolean value);

	/**
	 * bit 4
	 * 
	 * @param value
	 *            the value
	 */
	void d4(boolean value);

	/**
	 * bit 5
	 * 
	 * @param value
	 *            the value
	 */
	void d5(boolean value);

	/**
	 * bit 6
	 * 
	 * @param value
	 *            the value
	 */
	void d6(boolean value);

	/**
	 * bit 7
	 * 
	 * @param value
	 *            the value
	 */
	void d7(boolean value);
}
