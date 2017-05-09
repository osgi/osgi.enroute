package osgi.enroute.iot.toolkit;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.util.Binary;
import osgi.enroute.iot.gpio.util.Digital;
import osgi.enroute.iot.gpio.util.ICAdapter;
import osgi.enroute.iot.toolkit.And.AndConfig;

@Designate(ocd=AndConfig.class, factory=true)
@Component(service=IC.class, name="osgi.enroute.iot.toolkit.and")
public class And extends ICAdapter<Binary, Digital> implements Binary {
	boolean	a, b;

	@ObjectClassDefinition
	@interface AndConfig {
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

	@Reference
	protected
	void setCircuitBoard(CircuitBoard board) {
		super.setCircuitBoard(board);
	}

}
