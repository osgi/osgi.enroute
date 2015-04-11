package osgi.enroute.pi.provider;

import java.util.Map;

import osgi.enroute.dto.api.DTOs;
import osgi.enroute.iot.gpio.config.Config;
import osgi.enroute.iot.gpio.config.Direction;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

@Component(name="osgi.enroute.iot.gpio.config", designateFactory=Config.class)
public class GPIOConfig {
	DTOs		dtos;
	static GpioController controller = GpioFactory.getInstance();
	static Pin[] pins = {
		RaspiPin.GPIO_00,
		RaspiPin.GPIO_01,
		RaspiPin.GPIO_02,
		RaspiPin.GPIO_03,
		RaspiPin.GPIO_04,
		RaspiPin.GPIO_05,
		RaspiPin.GPIO_06,
		RaspiPin.GPIO_07,
		RaspiPin.GPIO_08,
		RaspiPin.GPIO_09,
		RaspiPin.GPIO_10,
		RaspiPin.GPIO_11,
		RaspiPin.GPIO_12,
		RaspiPin.GPIO_13,
		RaspiPin.GPIO_14,
		RaspiPin.GPIO_15,
		RaspiPin.GPIO_16,
		RaspiPin.GPIO_17,
		RaspiPin.GPIO_18,
		RaspiPin.GPIO_19,
		RaspiPin.GPIO_20		
	};
	
	@Activate
	void activate( Map<String,Object> map) throws Exception {
		Config c = dtos.convert(map).to(Config.class);
		if ( c.id() < 0 || c.id()>=pins.length)
			throw new IllegalArgumentException("Invalid pin " + c.id() + ". Must be be between 0 and "+ pins.length);
		
		Pin pin = pins[c.id()];
		
		switch(c.type()) {
		case analog:
			if ( c.direction() ==Direction.in)
				controller.provisionAnalogInputPin(pin,c.name());
			else
				controller.provisionAnalogOutputPin(pin,c.name(), c.value());
			break;
			
		case digital:
			
			PinPullResistance r;
			switch(c.pull()) {
			case down:
				r = PinPullResistance.PULL_DOWN;
				break;
			case up:
				r = PinPullResistance.PULL_UP;
				break;
			
			default:
			case off:
				r = PinPullResistance.OFF;
				break;
			}
			
			if ( c.direction() ==Direction.in)
				controller.provisionDigitalInputPin(pin,c.name(),r);
			else
				controller.provisionDigitalOutputPin(pin,c.name(), c.value() > 0.5 ? PinState.HIGH : PinState.LOW);
			break;
			
		case pwm:
			if ( c.direction() ==Direction.in)
				throw new IllegalArgumentException("PWM type can only be output");
			
			controller.provisionPwmOutputPin(pin,c.name(), (int)c.value());
			break;
			
		default:
			throw new IllegalArgumentException("Invalid configuration type for pin " + c.type());
		
		}
	}
}
