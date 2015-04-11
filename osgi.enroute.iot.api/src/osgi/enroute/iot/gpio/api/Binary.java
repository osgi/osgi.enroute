package osgi.enroute.iot.gpio.api;

/**
 * For ICs with 2 inputs
 *
 * @param <T> the 
 */
public interface Binary<A,B> {
	void a(A value) throws Exception;
	void b(B value) throws Exception;
}
