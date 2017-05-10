package osgi.enroute.iot.pi.provider;

import org.osgi.framework.ServiceRegistration;

import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.Pin;

/**
 * Registration of a GPIO pin.
 */
class Registration<T> {
	Class<T> type;
	ServiceRegistration<?> reg;
	T service;
	Pin pin;
	GpioPin gpioPin;
}
