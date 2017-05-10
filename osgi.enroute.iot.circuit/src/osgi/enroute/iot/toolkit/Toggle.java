package osgi.enroute.iot.toolkit;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import osgi.enroute.dto.api.DTOs;
import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.util.Digital;
import osgi.enroute.iot.gpio.util.ICAdapter;
import osgi.enroute.iot.toolkit.Toggle.FlipConfig;

@Designate(ocd=FlipConfig.class, factory=true)
@Component(service=IC.class, name="osgi.enroute.iot.toolkit.toggle")
public class Toggle extends ICAdapter<Digital, Digital> implements Digital {

	@ObjectClassDefinition
	@interface FlipConfig {
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

	@Reference
	protected void setDTOs(DTOs dtos) {
		super.setDTOs(dtos);
	}


	@Reference
	protected
	void setCircuitBoard(CircuitBoard board) {
		super.setCircuitBoard(board);
	}

}
