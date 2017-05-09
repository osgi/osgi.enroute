package osgi.enroute.iot.toolkit;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import aQute.lib.converter.Converter;
import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.util.Digital;
import osgi.enroute.iot.gpio.util.Enable;
import osgi.enroute.iot.gpio.util.ICAdapter;
import osgi.enroute.iot.toolkit.Schedule.ScheduleConfig;
import osgi.enroute.scheduler.api.CancellablePromise;
import osgi.enroute.scheduler.api.Scheduler;

@Designate(ocd=ScheduleConfig.class,factory=true)
@Component(service= IC.class,	 name="osgi.enroute.iot.toolkit.schedule")
public class Schedule extends ICAdapter<Enable, Digital> implements Enable {

	@ObjectClassDefinition
	@interface ScheduleConfig {
		String name();

		int duration();

		String cron() default "@hourly";

		boolean on();

		String service_pid();
	}

	class State {
		AtomicBoolean				closed	= new AtomicBoolean();
		Closeable					waitSchedule;
		CancellablePromise<Instant>	activeSchedule;
		private int					duration;
		AtomicBoolean				active	= new AtomicBoolean();

		public State(String cron, int duration) throws Exception {
			this.duration = duration;
			if (enabled) {
				waitSchedule = scheduler.schedule(this::tick, cron);
			}
		}

		public void close() {
			if (closed.getAndSet(true)) {
				if (waitSchedule != null)
					try {
						waitSchedule.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				if (activeSchedule != null)
					activeSchedule.cancel();
			}
		}

		private void tick() throws Exception {
			if ( active.getAndSet(true))
				return;

			on(this);
			activeSchedule = scheduler.after(duration * 1000L);
			activeSchedule.then((p) -> {
				off(State.this);
				active.set(false);
				return null;
			});
		}
	}

	ScheduleConfig	config;
	Scheduler		scheduler;
	boolean			enabled	= true;
	boolean			last;
	State			current;

	@Activate
	void activate(Map<String, Object> map) throws Exception {
		out().set(last = false);
		modified(map);
	}

	@Modified
	void modified(Map<String, Object> map) throws Exception {

		config = Converter.cnv(ScheduleConfig.class, map);
		cancel();

		current = new State(config.cron(), config.duration());
	}

	void on(State state) throws Exception {
		synchronized (this) {
			if (state != current)
				return;

			last = enabled;
			out().set(last);
		}
	}

	void off(State state) throws Exception {
		synchronized (this) {
			if (state != current)
				return;

			last = false;
			out().set(last);
		}
	}

	void cancel() throws Exception {
		State prior;
		synchronized (this) {
			prior = current;
			current = null;
			last = false;
			out().set(last);
		}
		if (prior != null)
			prior.close();
	}

	void changed() throws Exception {
		last &= enabled | config.on();
		out().set(last);
	}

	@Deactivate
	void deactivate(Map<String, Object> map) throws Exception {
		cancel();
		out().set(last = false);
	}

	@Override
	public void enbl(boolean value) throws Exception {
		enabled = value;
		changed();
	}

	@Reference
	void setScheduler(Scheduler sch) {
		this.scheduler = sch;
	}

	@Reference
	protected void setCircuitBoard(CircuitBoard board) {
		super.setCircuitBoard(board);
	}

}
