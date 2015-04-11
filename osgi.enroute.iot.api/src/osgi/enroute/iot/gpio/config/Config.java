package osgi.enroute.iot.gpio.config;

public interface Config {
	int id();
	String name();
	double value();
	PinType type();
	Direction direction(); 
	Pull pull();
}
