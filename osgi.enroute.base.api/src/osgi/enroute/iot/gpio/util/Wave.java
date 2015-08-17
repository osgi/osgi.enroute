package osgi.enroute.iot.gpio.util;


public interface Wave {
	void send( int[] widthsInµSec) throws Exception;
}
