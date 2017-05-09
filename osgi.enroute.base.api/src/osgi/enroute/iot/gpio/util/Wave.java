package osgi.enroute.iot.gpio.util;

/**
 * Defines a pin that accepts generates a wave form
 */
public interface Wave {
	/**
	 * Definition of a wave. The wave is a sequence of integers where each
	 * integer species a number of µseconds for each half period. The first
	 * integer specifies the up or active time, the second int specifies the
	 * wave down or inactive time. The wave description should therefore have an
	 * odd number of integers.
	 * 
	 * @param widthsInµSec
	 *            the widths of the pulse in µseconds
	 * @throws Exception
	 */
	void send(int[] widthsInµSec) throws Exception;
}
