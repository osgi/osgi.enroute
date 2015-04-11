package osgi.enroute.iot.gpio.config;

/**
 * Configuration for a General Purpose IO. The id is defined by the actual
 * underlying hardware.
 */
public interface Config {
	
	/**
	 * The GPIO number on the hardware
	 */
	int id();

	/**
	 * Device identifier if there are multiple devices.
	 */
	String device();

	/**
	 * The name of the pin
	 */
	String name();

	/**
	 * The initial value if it is an output pin.
	 */
	double value();

	/**
	 * The type of the pin.
	 */
	PinType type();

	/**
	 * If the pin is an input or an output
	 */
	Direction direction();

	/**
	 * Pull up/down resistor if output
	 */
	Pull pull();
}
