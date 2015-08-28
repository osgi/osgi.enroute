package osgi.enroute.iot.toolkit;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.util.Digital;
import osgi.enroute.iot.gpio.util.ICAdapter;

@Component(designateFactory=Not.Config.class, provide=IC.class, name="osgi.enroute.iot.toolkit.not")

public class Not extends ICAdapter<Digital, Digital> implements Digital {

	interface Config {
		String name();
	}
	@Override
	public void set(boolean value) throws Exception {
		out().set(!value);
	}

	@Reference
	protected
	void setCircuitBoard(CircuitBoard board) {
		super.setCircuitBoard(board);
	}


}
