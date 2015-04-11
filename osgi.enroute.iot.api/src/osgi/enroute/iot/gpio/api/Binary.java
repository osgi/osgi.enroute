package osgi.enroute.iot.gpio.api;

public interface Binary<T> {
	void a(T value) throws Exception;
	void b(T value) throws Exception;
}
