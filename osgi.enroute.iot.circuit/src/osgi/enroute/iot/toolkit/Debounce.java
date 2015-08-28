package osgi.enroute.iot.toolkit;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import aQute.lib.converter.Converter;
import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.util.Digital;
import osgi.enroute.iot.gpio.util.ICAdapter;
import osgi.enroute.iot.toolkit.Debounce.DebounceConfig;
import osgi.enroute.scheduler.api.Scheduler;

@Component(designateFactory = DebounceConfig.class, provide = IC.class, name = "osgi.enroute.iot.toolkit.debounce")
public class Debounce extends ICAdapter<Digital, Digital> implements Digital {

	private AtomicBoolean	busy	= new AtomicBoolean();
	private Scheduler		scheduler;
	private boolean			state;
	private int				period;

	interface DebounceConfig {
		String name();

		int period(int deflt);
	}

	@Activate
	void activate(Map<String, Object> config) throws Exception {
		DebounceConfig cfg = Converter.cnv(DebounceConfig.class, config);
		period = cfg.period(100);
	}

	@Override
	public synchronized void set(boolean value) throws Exception {
		this.state = value;

		if (busy.getAndSet(true) == false) {
			out().set(state);
			scheduler.after(() -> {
				out().set(state);
				busy.set(false);
			}, period);
		}
	}

	@Reference
	protected void setCircuitBoard(CircuitBoard board) {
		super.setCircuitBoard(board);
	}

	@Reference
	void setScheduler(Scheduler s) {
		this.scheduler = s;
	}

}
