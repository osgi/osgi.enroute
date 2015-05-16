package osgi.enroute.iot.gpio.api;

public interface CircuitBoard {
	boolean fire( IC ic, String pin, Object value);
}
