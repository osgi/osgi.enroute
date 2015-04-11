package osgi.enroute.iot.gpio.api;

import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.api.Pin;

public class Or extends IC<Binary<Boolean>,Pin<Boolean>> implements Binary<Boolean>{
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
		if (a || b != out)
			out().set(out = !out);
	}


}
