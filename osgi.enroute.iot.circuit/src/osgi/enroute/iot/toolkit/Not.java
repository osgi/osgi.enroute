package osgi.enroute.iot.toolkit;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.util.Digital;
import osgi.enroute.iot.gpio.util.ICAdapter;

@Designate(ocd=Not.Config.class, factory=true)
@Component(service=IC.class, name="osgi.enroute.iot.toolkit.not")
public class Not extends ICAdapter<Digital, Digital> implements Digital {

	@ObjectClassDefinition
	@interface Config {
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
