package osgi.enroute.iot.gpio.api;

/**
 * An IC that acts as a General Purpose Input from the real hardware. If the
 * hardware changes, the output is signalled.
 */
public abstract class GPI extends IC<Void, Pin<Boolean>> implements
		Pin<Boolean> {

}
