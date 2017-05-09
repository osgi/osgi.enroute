package osgi.enroute.iot.pi.provider;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.StopBits;

import aQute.bnd.annotation.metatype.Meta;
import osgi.enroute.dto.api.DTOs;
import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.util.Analog;
import osgi.enroute.iot.gpio.util.GPI;
import osgi.enroute.iot.gpio.util.GPO;
import osgi.enroute.iot.gpio.util.ICAdapter;
import osgi.enroute.iot.pi.provider.Model2B_Rev1Impl.Model2B_Rev1;

/**
 * This component is initializing the Raspberry Pi. It should work for Model 1
 * B+ and Model 2B
 *
 * TODO Refactor to use base class
 */
@Designate(ocd=Model2B_Rev1.class,factory=false)
@Component
public class Model2B_Rev1Impl {

	public enum SPI {
		ignore, none, in, out, spi;
	}

	public enum GPIO {
		ignore, none, in, out
	};

	public enum I2C {
		ignore, none, in, out, i2c
	};

	public enum PWM {
		ignore, none, in, out, pwm
	};

	public enum UART {
		ignore, none, in, out, uart
	};

	public enum Level {
		off, low, high
	};

	public enum GPCLK {
		ignore, none, in, out, clock;
	}

	public enum PCM {
		ignore, none, low, high, pcm;
	}

	@ObjectClassDefinition(description = "Raspberry Pi 2 Model B – The Raspberry Pi 2 Model B is the second "
			+ "generation Raspberry Pi. It replaced the original Raspberry Pi 1 Model B+ in February 2015. "
			+ "The Raspberry Pi 2 has an identical form factor to the previous (Pi 1) Model B+ and has "
			+ "complete compatibility with Raspberry Pi 1", id = "Pi_2_B_OCD", name = "Pi 2 Model B")
	public @interface Model2B_Rev1 {
		// 01 3.3 V
		// 02 5 V

		// 03 GPIO 8
		@Meta.AD(description = "GPIO 8 (or I2C SDA1)")
		I2C _03();

		@Meta.AD(description = "GPIO 8 Level")
		Level _03_Level();

		// 04 5 V

		// 05 GPIO 9
		@Meta.AD(description = "GPIO 9 (or I2C SCL1)")
		GPIO _05();

		@Meta.AD(description = "GPIO 5 Level")
		Level _05_Level();

		// 06 Ground

		// 07 GPIO 7
		@Meta.AD(description = "GPIO 7 (or GPCLK0)")
		GPCLK _07();

		@Meta.AD(description = "GPIO 7 Level")
		Level _07_Level();

		// 08 GPIO 15 or TX (UART)
		@Meta.AD(description = "GPIO 15 or TX (UART)")
		UART _08();

		@Meta.AD(description = "GPIO 15 Level")
		Level _08_Level();

		@Meta.AD(description = "Baud Rate for UART")
		com.pi4j.io.serial.Baud _08_Baud();

		@Meta.AD(description = "Data bits for UART")
		com.pi4j.io.serial.DataBits _08_DataBits();

		@Meta.AD(description = "Parity for UART")
		com.pi4j.io.serial.Parity _08_Parity();

		@Meta.AD(description = "Stop Bits for UART")
		com.pi4j.io.serial.StopBits _08_StopBits();

		// 09 Ground

		// 10 GPIO 16 or RX (UART)

		@Meta.AD(description = "GPIO 16 or RX as defined by pin 08")
		GPIO _10();

		@Meta.AD(description = "GPIO 16 Level")
		Level _10_Level();

		// 11 GPIO 0
		@Meta.AD(description = "GPIO 0")
		GPIO _11();

		@Meta.AD(description = "GPIO 0 Level")
		Level _11_Level();

		// 12 GPIO 1 or PCM_CLK/PWWM0
		@Meta.AD(description = "GPIO 1 or PCM_CLOCK or PWM0")
		PWM _12();

		@Meta.AD(description = "GPIO 1 Level")
		Level _12_Level();

		// 13 GPIO 2
		@Meta.AD(description = "GPIO 2")
		GPIO _13();

		@Meta.AD(description = "GPIO 2 Level")
		Level _13_Level();

		// 14 Ground

		// 15 GPIO 3
		@Meta.AD(description = "GPIO 3")
		GPIO _15();

		@Meta.AD(description = "GPIO 3 Level")
		Level _15_Level();

		// 16 GPIO 4
		@Meta.AD(description = "GPIO 4")
		GPIO _16();

		@Meta.AD(description = "GPIO 4 Level")
		Level _16_Level();

		// 17 3.3 V

		// 18 GPIO 5
		@Meta.AD(description = "GPIO 5")
		GPIO _18();

		@Meta.AD(description = "GPIO 5 Level")
		Level _18_Level();

		// 19 GPIO 12 or MOSI (SPI)
		@Meta.AD(description = "GPIO 12 or MOSI (SPI)")
		SPI _19();

		@Meta.AD(description = "GPIO 12 Level")
		Level _19_Level();

		// 20 Ground

		// 21 GPIO 13 or MOSI (SPI)
		@Meta.AD(description = "GPIO 13 or MISO (SPI)")
		GPIO _21();

		@Meta.AD(description = "GPIO 13 Level")
		Level _21_Level();

		// 22 GPIO 6
		@Meta.AD(description = "GPIO 6")
		GPIO _22();

		@Meta.AD(description = "GPIO 6 Level")
		Level _22_Level();

		// 23 GPIO 14
		@Meta.AD(description = "GPIO 14 or SCLK (SPI)")
		GPIO _23();

		@Meta.AD(description = "GPIO 14 Level")
		Level _23_Level();

		// 24 GPIO 10 or CE0 (SPI)
		@Meta.AD(description = "GPIO 10 or CE0 (SPI)")
		GPIO _24();

		@Meta.AD(description = "GPIO 10 Level")
		Level _24_Level();

		// 25 Ground

		// 26 GPIO 11
		@Meta.AD(description = "GPIO 11 or CE1 (SPI)")
		GPIO _26();

		@Meta.AD(description = "GPIO 11 Level")
		Level _26_Level();

		// 27 SDA0 (I2C ID EEPROM)

		// 28 SCL0 (I2C ID EEPROM)

		// 29 GPIO 21 or GPCLK1
		@Meta.AD(description = "GPIO 21 or GPCLK1")
		GPCLK _29();

		@Meta.AD(description = "GPIO 21 Level")
		Level _29_Level();

		// 30 Ground

		// 31 GPIO 22 or GPCLK2
		@Meta.AD(description = "GPIO 22 or GPCLK2")
		GPCLK _31();

		@Meta.AD(description = "GPIO 22 Level")
		Level _31_Level();

		// 32 GPIO 26 or PWM0
		@Meta.AD(description = "GPIO 26 or PWM0")
		PWM _32();

		@Meta.AD(description = "GPIO 26 Level")
		Level _32_Level();

		// 33 GPIO 23 or PWM1
		@Meta.AD(description = "GPIO 23 or PWM1")
		PWM _33();

		@Meta.AD(description = "GPIO 23 Level")
		Level _33_Level();

		// 34 Ground

		// 35 GPIO 24 or PCM_FS/PWM1
		@Meta.AD(description = "GPIO 24 or PCM_FS/PWM1")
		PWM _35();

		@Meta.AD(description = "GPIO 24 Level")
		Level _35_Level();

		// 36 GPIO 27
		@Meta.AD(description = "GPIO 27")
		GPIO _36();

		@Meta.AD(description = "GPIO 27 Level")
		Level _36_Level();

		// 37 GPIO 25
		@Meta.AD(description = "GPIO 25")
		GPIO _37();

		@Meta.AD(description = "GPIO 25 Level")
		Level _37_Level();

		// 38 GPIO 28 or PCM_DIN
		@Meta.AD(description = "GPIO 28 or PCM_DIN")
		PCM _38();

		@Meta.AD(description = "GPIO 28 Level")
		Level _38_Level();

		// 39 ground

		// 40 GPIO 29
		@Meta.AD(description = "GPIO 29 or PCM_DOUT")
		GPIO _40();

		@Meta.AD(description = "GPIO 29 Level")
		Level _40_Level();
	}

	private GpioController gpio;
	private DTOs dtos;
	private Map<Pin, Registration<?>> registrations = new HashMap<Pin, Registration<?>>();
	private BundleContext context;
	private CircuitBoard board;

	@Activate
	void activate(Map<String, Object> config, BundleContext context) throws Exception {
		this.context = context;
		try {
			Model2B_Rev1 c = dtos.convert(config).to(Model2B_Rev1.class);

			if (c._03() == I2C.i2c)
				i2c();
			else {
				gpio(RaspiPin.GPIO_08, c._03(), c._03_Level(), "GPIO08");
				gpio(RaspiPin.GPIO_09, c._05(), c._05_Level(), "GPIO09");
			}

			if (c._07() == GPCLK.clock)
				clock(RaspiPin.GPIO_07);
			else
				gpio(RaspiPin.GPIO_07, c._07(), c._07_Level(), "GPIO07");

			if (c._08() == UART.uart) {
				serial(c._08_Baud(), c._08_DataBits(), c._08_Parity(), c._08_StopBits());
			} else {
				gpio(RaspiPin.GPIO_15, c._08(), c._08_Level(), "GPIO15");
				gpio(RaspiPin.GPIO_16, c._10(), c._10_Level(), "GPIO16");
			}

			gpio(RaspiPin.GPIO_00, c._11(), c._11_Level(), "GPIO00");

			if (c._12() == PWM.pwm) {
				pwm(RaspiPin.GPIO_01);
			} else {
				gpio(RaspiPin.GPIO_01, c._12(), c._12_Level(), "GPIO01");
			}

			gpio(RaspiPin.GPIO_02, c._13(), c._13_Level(), "GPIO02");
			gpio(RaspiPin.GPIO_03, c._15(), c._15_Level(), "GPIO03");
			gpio(RaspiPin.GPIO_04, c._16(), c._16_Level(), "GPIO04");
			gpio(RaspiPin.GPIO_05, c._18(), c._18_Level(), "GPIO05");

			// 19 GPIO 12 or MOSI (SPI)
			if (c._19() == SPI.spi) {
				// spi();
			} else {
				gpio(RaspiPin.GPIO_12, c._19(), c._19_Level(), "GPIO12");
				gpio(RaspiPin.GPIO_13, c._21(), c._21_Level(), "GPIO13");
				gpio(RaspiPin.GPIO_14, c._23(), c._23_Level(), "GPIO14");
				gpio(RaspiPin.GPIO_10, c._24(), c._24_Level(), "GPIO10");
				gpio(RaspiPin.GPIO_11, c._26(), c._26_Level(), "GPIO11");
			}

			gpio(RaspiPin.GPIO_06, c._22(), c._22_Level(), "GPIO06");

			if (c._29() == GPCLK.clock)
				clock(RaspiPin.GPIO_21);
			else
				gpio(RaspiPin.GPIO_21, c._29(), c._29_Level(), "GPIO21");

			if (c._31() == GPCLK.clock)
				clock(RaspiPin.GPIO_22);
			else
				gpio(RaspiPin.GPIO_22, c._31(), c._31_Level(), "GPIO22");

			if (c._32() == PWM.pwm)
				pwm(RaspiPin.GPIO_26);
			else
				gpio(RaspiPin.GPIO_26, c._32(), c._32_Level(), "GPIO26");

			if (c._33() == PWM.pwm)
				pwm(RaspiPin.GPIO_23);
			else
				gpio(RaspiPin.GPIO_23, c._33(), c._33_Level(), "GPIO23");

			if (c._35() == PWM.pwm)
				pwm(RaspiPin.GPIO_24);
			else
				gpio(RaspiPin.GPIO_24, c._35(), c._35_Level(), "GPIO24");

			gpio(RaspiPin.GPIO_27, c._36(), c._36_Level(), "GPIO27");
			gpio(RaspiPin.GPIO_25, c._37(), c._37_Level(), "GPIO25");

			if (c._38() == PCM.pcm) {
				pcm();
			} else {
				gpio(RaspiPin.GPIO_28, c._38(), c._38_Level(), "GPIO28");
				gpio(RaspiPin.GPIO_29, c._40(), c._40_Level(), "GPIO29");
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Deactivate
	void deactivate() {
		for (Registration<?> r : registrations.values()) {
			r.reg.unregister();
		}
	}

	private void pcm() {
		throw new IllegalArgumentException("PCM not yet implemented");
	}

	class PWMO extends ICAdapter<Analog, Void> implements Analog {

		private GpioPinPwmOutput pwm;

		public PWMO(GpioPinPwmOutput pwm) {
			this.pwm = pwm;
		}

		@Override
		public void set(double value) {
			value = value * 1024;
			if (value < 0)
				value = 0;
			else if (value > 1024)
				value = 1024;

			pwm.setPwm((int) Math.round(value));
		}

	}

	private void pwm(Pin pin) {
		GpioPinPwmOutput pwm = this.gpio.provisionPwmOutputPin(pin);

		unprovision(pin);
		PWMO pwmo = new PWMO(pwm);

		register(pin, PWMO.class, pwmo, pwm, pin.getName());
	}

	private void serial(Baud baud, DataBits dataBits, Parity parity, StopBits stopBits) {
		throw new IllegalArgumentException("Serial not yet implemented");
	}

	private void clock(Pin pin) {
		throw new IllegalArgumentException("Clock not yet implemented");
	}

	private void i2c() {
		throw new IllegalArgumentException("I2C not yet implemented");
	}

	private void gpio(Pin pin, Enum<?> en, Level level, String name) {
		if (en == null)
			return;

		GPIO gpio = Enum.valueOf(GPIO.class, en.name());
		if (gpio == GPIO.ignore)
			return;

		unprovision(pin);

		switch (gpio) {
		case in:
			GpioPinDigitalInput digitalIn = this.gpio.provisionDigitalInputPin(pin);
			switch (level) {
			case high:
				digitalIn.setPullResistance(PinPullResistance.PULL_UP);
				break;

			case low:
				digitalIn.setPullResistance(PinPullResistance.PULL_DOWN);
				break;

			default:
			case off:
				digitalIn.setPullResistance(PinPullResistance.OFF);
				break;
			}

			GPI gpi = new GPI(name, board, dtos);
			digitalIn.addListener((GpioPinListenerDigital) (GpioPinDigitalStateChangeEvent event) -> {
				try {
					boolean high = event.getState().isHigh();
					board.fire(gpi, "set", high);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			register(pin, GPI.class, gpi, digitalIn, name);
			board.fire(gpi, "set", digitalIn.getState());
			break;

		case out:
			unprovision(pin);
			GpioPinDigitalOutput digitalOut = this.gpio.provisionDigitalOutputPin(pin);

			boolean invert = false;
			switch (level) {
			case high:
				digitalOut.setState(true);
				invert = true; // assume the off value is high
				// therefore invert
				break;

			default:
			case off:
			case low:
				digitalOut.setState(false);
				invert = false; // assume the off value is low
				break;
			}
			boolean inverted = invert;
			GPO gpo = new GPO(name, board, dtos) {
				@Override
				public void set(boolean value) throws Exception {
					digitalOut.setState(value ^ inverted);
				}
			};
			register(pin, GPO.class, gpo, digitalOut, name);
			break;

		default:
		case none:
			return;
		}
	}

	private <T extends ICAdapter<?, ?>> void register(Pin pin, Class<T> type, T gp, GpioPin gpioPin, String name) {
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put(Constants.SERVICE_PID, name);
		Registration<T> r = new Registration<T>();
		r.type = type;
		r.service = gp;
		r.pin = pin;
		r.reg = context.registerService(new String[] { IC.class.getName(), type.getName() }, gp, props);
		r.gpioPin = gpioPin;
		Registration<?> prev = registrations.put(pin, r);
		if (prev != null)
			prev.reg.unregister();
	}

	private void unprovision(Pin pin) {
		Registration<?> r = registrations.remove(pin);
		if (r != null) {
			if (r.reg != null)
				r.reg.unregister();
			gpio.unprovisionPin(r.gpioPin);
		} else {
			for (GpioPin e : gpio.getProvisionedPins()) {
				if (e.getPin().equals(pin)) {
					gpio.unprovisionPin(e);
					break;
				}
			}
		}
	}

	// TODO Must handle the compatibility rules
	@Reference(target = "(|(board.type=Model2B_Rev1)(board.type=ModelB_Plus_Rev1))")
	void setGpioController(GpioController controller) {
		this.gpio = controller;
	}

	@Reference
	void setDTOs(DTOs dtos) {
		this.dtos = dtos;
	}

	@Reference
	void setCircuitBoard(CircuitBoard board) {
		this.board = board;
	}

}