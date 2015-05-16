package osgi.enroute.iot.circuit.toolkit;

import osgi.enroute.iot.circuit.toolkit.And.AndConfig;
import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.util.Digital;
import osgi.enroute.iot.gpio.util.ICAdapter;
import aQute.bnd.annotation.component.Component;

@Component(designateFactory=AndConfig.class, provide=IC.class)
public class And extends ICAdapter<Binary, Digital> implements Binary {
	boolean	a, b;
	interface AndConfig {
		String name();
	}
	
	@Override
	public synchronized void a(boolean value) throws Exception {
		a = value;
		update();
	}

	@Override
	public synchronized void b(boolean value) throws Exception {
		b = value;
		update();
	}

	private void update() throws Exception {
		out().set(a && b);
	}
}
