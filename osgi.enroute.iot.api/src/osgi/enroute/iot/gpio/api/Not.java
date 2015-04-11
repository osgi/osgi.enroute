package osgi.enroute.iot.gpio.api;


/**
 * A simple implementation of an Inverter circuit.
 */
public class Not extends IC<Pin<Boolean>,Pin<Boolean>> implements Pin<Boolean>{
	volatile Boolean out;

	@Override
	public void set(Boolean value) throws Exception {
		out = !value;
		out().set(out);
	}


	@Override
	public void flush(Pin<Boolean> output) throws Exception {
		output.set(out);
	}

}
