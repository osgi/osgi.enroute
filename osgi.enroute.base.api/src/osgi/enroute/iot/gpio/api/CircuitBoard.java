package osgi.enroute.iot.gpio.api;

/**
 * Represents the circuit board on which the ICs are connected.
 */
public interface CircuitBoard {
	/**
	 * Define a new value for an IC
	 * 
	 * @param ic
	 *            the IC that originates the signal
	 * @param pin
	 *            the pin that originates the signal
	 * @param value
	 *            The value to set
	 * @return A boolean if there were any listeners
	 */
	boolean fire(IC ic, String pin, Object value);
}
