package osgi.enroute.iot.toolkit;

import java.io.Closeable;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import aQute.lib.converter.Converter;
import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.util.Digital;
import osgi.enroute.iot.gpio.util.ICAdapter;
import osgi.enroute.iot.gpio.util.Octionary;
import osgi.enroute.iot.toolkit.Counter.CounterConfig;
import osgi.enroute.scheduler.api.Scheduler;

@Designate(ocd=CounterConfig.class,factory=true)
@Component(service = IC.class, name="osgi.enroute.iot.toolkit.ctr8")
public class Counter extends ICAdapter<Digital, Octionary> implements Digital {

	@ObjectClassDefinition
	@interface CounterConfig {
		String name();

		int period();

		int modulo();

		String service_pid();
	}

	CounterConfig	config;
	Scheduler		scheduler;
	Closeable		schedule;
	int				modulo;
	int				counter	= -1;
	boolean			enable	= true;

	@Activate
	void activate(Map<String, Object> map) throws Exception {
		config = Converter.cnv(CounterConfig.class, map);
		int period = config.period();
		if (period <= 0)
			period = 100;

		modulo = config.modulo();
		if (modulo <= 0) {
			modulo = 256;
		}

		schedule = scheduler.schedule(this::tick, period, period);
		super.setDeviceId(config.service_pid());
	}

	@Deactivate
	void deactivate(Map<String, Object> map) throws Exception {
		schedule.close();
	}

	private void tick() {
		if (!enable)
			return;

		int previous = counter;

		counter++;
		if (counter >= modulo)
			counter = 0;

		int changes = counter ^ previous;

		int mask = 1;
		for (int i = 0; i < 8; i++) {
			boolean change = (mask & changes) != 0;
			if (change) {
				boolean value = (counter & mask) != 0;

				switch (i) {
				case 0:
					out().d0(value);
					break;

				case 1:
					out().d1(value);
					break;

				case 2:
					out().d2(value);
					break;

				case 3:
					out().d3(value);
					break;

				case 4:
					out().d4(value);
					break;

				case 5:
					out().d5(value);
					break;

				case 6:
					out().d6(value);
					break;

				case 7:
					out().d7(value);
					break;
				}
			}
			mask <<= 1;
		}
	}

	@Override
	public void set(boolean value) throws Exception {
		enable = value;
	}

	@Reference
	void setScheduler(Scheduler sch) {
		this.scheduler = sch;
	}

	@Reference
	protected
	void setCircuitBoard(CircuitBoard board) {
		super.setCircuitBoard(board);
	}


}
