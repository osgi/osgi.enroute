package osgi.enroute.iot.gpio.api;

/**
 * Defines a single input or output pin on an IC.
 * 
 * @param <T> the data type. Must be a DTO type
 */
public interface Pin<T> {
	void set(T value) throws Exception;
}
