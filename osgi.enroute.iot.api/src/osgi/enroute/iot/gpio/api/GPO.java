package osgi.enroute.iot.gpio.api;

/**
 * IC that acts as a general purpose output of the real hardware. Calling the
 * input method will set the output.
 */
public abstract class GPO extends IC<Pin<Boolean>, Void> {

}
