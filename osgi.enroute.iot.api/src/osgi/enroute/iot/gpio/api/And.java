package osgi.enroute.iot.gpio.api;


/**
 * A simple implementation of an AND circuit with an a & b input.
 */
public class And extends IC<Binary<Boolean,Boolean>,Pin<Boolean>> implements Binary<Boolean,Boolean>{
	volatile Boolean a;
	volatile Boolean b;
	volatile Boolean out;

	@Override
	public void a(Boolean value) throws Exception {
		a = value;
		update();
	}

	@Override
	public void b(Boolean value) throws Exception {
		b = value;
		update();
	}

	private synchronized void update() throws Exception {
		if (a && b != out)
			out().set(out = !out);
	}

	@Override
	public void flush(Pin<Boolean> output) throws Exception {
		output.set(out);
	}

}
