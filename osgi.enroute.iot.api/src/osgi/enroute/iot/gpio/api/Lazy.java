package osgi.enroute.iot.gpio.api;

/**
 * A special data value. The value will only be calculated when it is actually
 * used. The Lazy maybe used anywhere a value (normally DTO types) is used.
 * 
 * @param <T>
 *            the actual data type (a DTO type).
 */
public interface Lazy<T> {
	T eval();
}
