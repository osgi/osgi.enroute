package osgi.enroute.iot.circuit.toolkit;

import osgi.enroute.iot.circuit.toolkit.Flip.FlipConfig;
import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.util.Digital;
import osgi.enroute.iot.gpio.util.ICAdapter;
import aQute.bnd.annotation.component.Component;

@Component(designateFactory=FlipConfig.class, provide=IC.class)
public class Flip extends ICAdapter<Digital, Digital> implements Digital {

	interface FlipConfig {
		String name();
	}
	boolean state;
	
	@Override
	public synchronized void set(boolean value) throws Exception {
		if ( value == false ) {
			state = !state;
		}
		out().set(state);
	}
}
