package osgi.enroute.iot.gpio.util;

/**
 * A Data IC pin. This will handle byte[] data
 */
public interface Data {
	/**
	 * The data
	 * 
	 * @param data
	 *            the data
	 * @throws Exception
	 */
	void stream(byte[] data) throws Exception;
}
