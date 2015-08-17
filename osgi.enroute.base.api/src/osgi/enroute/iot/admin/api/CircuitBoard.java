package osgi.enroute.iot.admin.api;

public interface CircuitBoard {
	boolean fire( IC ic, String pin, Object value);
}
