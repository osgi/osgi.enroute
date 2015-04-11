package osgi.enroute.iot.gpio.api;

public interface Pin<T> {
	String NAME = "name";
	void set(T value) throws Exception;
}
