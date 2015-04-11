package osgi.enroute.iot.emulator.provider;

import org.osgi.framework.ServiceRegistration;

import osgi.enroute.iot.gpio.api.IC;

public class GPIO {
	enum Direction { in, out };
	
	int nr;
	String name;
	Direction direction;
	boolean pullup;

	IC<?,?> ic;
	ServiceRegistration<?> registration;
}
