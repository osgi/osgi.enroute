package osgi.enroute.iot.gpio.util;

/**
 * A 2 pin Binary input or output
 */
public interface Binary {
	/**
	 * The primary pin
	 * 
	 * @param value
	 *            the value
	 * @throws Exception
	 */
	void a(boolean value) throws Exception;

	/**
	 * The secondary pin
	 * 
	 * @param value
	 *            the value
	 * @throws Exception
	 */
	void b(boolean value) throws Exception;
}

