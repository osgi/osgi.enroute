package osgi.enroute.base.timer.provider;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * Provides java.util.Timer as a service in an OSGi environment. This service is
 * instantiated once for every requesting bundle by the service factory. When
 * the requesting bundle ungets this service (either explicitly or due to this
 * bundle's leaving the active state), the underlying timer is canceled and the
 * service instance can be reclaimed by the garbage collector.
 * 
 * @see <a
 *      href="http://docs.oracle.com/javase/1.5.0/docs/api/java/util/Timer.html">java.util.Timer</a>
 */
@Component(service = java.util.Timer.class, property = "service.ranking=-1000")
public class TimerImpl extends java.util.Timer {
	static Timer	actTimer	= new Timer("enRoute :: Timer", true);
	Executor		executor;
	List<TimerTask>	tasks		= new CopyOnWriteArrayList<>();
	Timer			timer;

	@Deactivate
	void deactivate() {
		for (TimerTask t : tasks)
			t.cancel();
		timer = null;
	}

	@Activate
	void activate() {
		timer = actTimer;
	}

	public void schedule(TimerTask task, long delay) {
		timer.schedule(wrap(task), delay);
	}

	public void schedule(TimerTask task, Date time) {
		timer.schedule(wrap(task), time);
	}

	public void schedule(TimerTask task, long delay, long period) {
		timer.schedule(wrap(task), delay, period);
	}

	public void schedule(TimerTask task, Date firstTime, long period) {
		timer.schedule(wrap(task), firstTime, period);
	}

	public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
		timer.scheduleAtFixedRate(wrap(task), delay, period);
	}

	public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period) {
		timer.scheduleAtFixedRate(wrap(task), firstTime, period);
	}

	/*
	 * Run the timers tasks in a background thread and not directly since this
	 * timer is shared of course.
	 * @param task
	 * @return
	 */
	private TimerTask wrap(final TimerTask task) {
		return new TimerTask() {
			volatile Executor	executor	= TimerImpl.this.executor;

			@Override
			public void run() {
				if (executor != null)
					executor.execute(task);
			}

			public boolean cancel() {
				executor = null;
				return super.cancel();
			}
		};
	}

	public void cancel() {
		throw new UnsupportedOperationException(
				"This is a shared enRoute timer, you cannot cancel such timers. These timers are canceled when all using components are deactivated");
	}

	public int purge() {
		throw new UnsupportedOperationException("This is a shared enRoute timer, you cannot purge such timers");
	}

	@Reference
	void setExecutor(Executor executor) {
		this.executor = executor;
	}
}