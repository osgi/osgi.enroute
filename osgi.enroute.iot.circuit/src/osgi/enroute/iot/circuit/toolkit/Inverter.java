package osgi.enroute.iot.circuit.toolkit;

import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.util.Digital;
import osgi.enroute.iot.gpio.util.ICAdapter;
import aQute.bnd.annotation.component.Component;

@Component(designateFactory=Inverter.Config.class, provide=IC.class)

public class Inverter extends ICAdapter<Digital, Digital> implements Digital {

	interface Config {
		String name();
	}
	@Override
	public void set(boolean value) throws Exception {
		out().set(!value);
	}

}
