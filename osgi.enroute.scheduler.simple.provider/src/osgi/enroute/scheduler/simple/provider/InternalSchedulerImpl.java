package osgi.enroute.scheduler.simple.provider;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.lib.converter.Converter;
import osgi.enroute.scheduler.api.CancelException;
import osgi.enroute.scheduler.api.CronJob;
import osgi.enroute.scheduler.api.Scheduler;
import osgi.enroute.scheduler.api.SchedulerConstants;
import osgi.enroute.scheduler.api.TimeoutException;

/**
 *
 */
@ProvideCapability(ns = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, name = SchedulerConstants.SCHEDULER_SPECIFICATION_NAME, version = SchedulerConstants.SCHEDULER_SPECIFICATION_VERSION)
@Component(name = "osgi.enroute.scheduler.simple", service = InternalSchedulerImpl.class, immediate = true)
public class InternalSchedulerImpl implements Scheduler {
	final List<Cron<?>>			crons	= new ArrayList<>();
	final Logger				logger	= LoggerFactory
			.getLogger(InternalSchedulerImpl.class);

	Clock						clock	= Clock.systemDefaultZone();
	ScheduledExecutorService	executor;

	@Deactivate
	synchronized void deactivate() {
		if (executor != null) {
			List<Runnable> shutdownNow = executor.shutdownNow();
			if (shutdownNow != null && shutdownNow.size() > 0)
				logger.warn("Shutdown executables " + shutdownNow);
		}
	}

	private synchronized ScheduledExecutorService getExecutor() {
		if (executor == null) {
			executor = Executors.newScheduledThreadPool(10);
		}
		return executor;
	}

	@Override
	public CancellablePromiseImpl<Instant> after(long ms) {
		Deferred<Instant> deferred = new Deferred<>();
		Instant start = Instant.now();
		ScheduledFuture<?> schedule = getExecutor().schedule(() -> {
			deferred.resolve(start);
		}, ms, TimeUnit.MILLISECONDS);

		return new CancellablePromiseImpl<Instant>(deferred.getPromise()) {
			public boolean cancel() {
				try {
					return schedule.cancel(true);
				} catch (Exception e) {
					return false;
				}
			}
		};
	}

	@Override
	public <T> CancellablePromiseImpl<T> after(Callable<T> callable, long ms) {
		Deferred<T> deferred = new Deferred<>();

		ScheduledFuture<?> schedule = getExecutor().schedule(() -> {
			try {
				deferred.resolve(callable.call());
			} catch (Throwable e) {
				deferred.fail(e);
			}
		}, ms, TimeUnit.MILLISECONDS);

		return new CancellablePromiseImpl<T>(deferred.getPromise()) {
			public boolean cancel() {
				try {
					return schedule.cancel(true);
				} catch (Exception e) {
					return false;
				}
			}
		};
	}

	public <T> Success<T, T> delay(long ms) {
		return (p) -> {
			Deferred<T> deferred = new Deferred<T>();
			after(ms).then((pp) -> {
				deferred.resolve(p.getValue());
				return null;
			});
			return deferred.getPromise();
		};
	}

	static class Unique {
		AtomicBoolean done = new AtomicBoolean();

		interface RunnableException {
			public void run() throws Exception;
		}

		boolean once(RunnableException o) throws Exception {
			if (done.getAndSet(true) == false) {
				o.run();
				return true;
			} else
				return false;
		}
	}

	/**
	 * Return a new Promise that will fail after timeout ms with a
	 * {@link TimeoutException}
	 */
	// @Override
	public <T> CancellablePromiseImpl<T> before(Promise<T> promise,
			long timeout) {
		Deferred<T> d = new Deferred<T>();
		Unique only = new Unique();

		after(timeout).then((p) -> {
			only.once(() -> d.fail(TimeoutException.SINGLETON));
			return null;
		});

		promise.then((p) -> {
			only.once(() -> d.resolve(p.getValue()));
			return null;
		}, (p) -> {
			only.once(() -> d.fail(p.getFailure()));
		});
		return new CancellablePromiseImpl<T>(d.getPromise()) {
			public boolean cancel() {
				try {
					return only.once(() -> d.fail(CancelException.SINGLETON));
				} catch (Exception e) {
					return false;
				}
			}
		};
	}

	static abstract class Schedule {
		volatile CancellablePromiseImpl<?>	promise;
		volatile boolean					canceled;
		long								start	= System
				.currentTimeMillis();
		Throwable							exception;

		abstract long next();

		abstract void doIt() throws Exception;
	}

	static class PeriodSchedule extends Schedule {
		long						last;
		PrimitiveIterator.OfLong	iterator;
		long						rover;
		RunnableWithException		runnable;

		long next() {
			if (iterator.hasNext())
				last = iterator.nextLong();

			return rover += last;
		}

		void doIt() throws Exception {
			runnable.run();
		}
	}

	@Override
	public Closeable schedule(RunnableWithException r, long first, long... ms) {
		PeriodSchedule s = new PeriodSchedule();
		s.iterator = Arrays.stream(ms).iterator();
		s.runnable = r;
		s.rover = System.currentTimeMillis() + first;
		s.last = first;
		schedule(s, first + System.currentTimeMillis());
		return () -> {
			s.canceled = true;
			s.promise.cancel();
		};
	}

	private void schedule(Schedule s, long epochTime) {
		s.promise = at(() -> {
			try {
				s.doIt();
			} catch (Throwable t) {
				if (s.exception != null)
					logger.warn("Schedule failed " + s, t);
				s.exception = t;
			}

			schedule(s, s.next());
			return null;
		}, epochTime);
		if (s.canceled)
			s.promise.cancel();
	}

	class ScheduleCron<T> extends Schedule {
		CronAdjuster			cron;
		CronJob<T>				job;
		RunnableWithException	runnable;
		T						env;

		@Override
		long next() {
			ZonedDateTime now = ZonedDateTime.now(clock);
			ZonedDateTime next = now.with(cron);
			return next.toInstant().toEpochMilli();
		}

		void doIt() throws Exception {
			if (runnable != null)
				runnable.run();
			else
				job.run(env);
		}
	}

	@Override
	public Closeable schedule(RunnableWithException r, String cronExpression)
			throws Exception {
		ScheduleCron<Void> s = new ScheduleCron<>();
		s.cron = new CronAdjuster(cronExpression);
		s.runnable = r;
		schedule(s, s.next());
		return () -> {
			s.canceled = true;
			s.promise.cancel();
		};
	}

	@Override
	public <T> Closeable schedule(Class<T> type, CronJob<T> job,
			String cronExpression) throws Exception {
		ScheduleCron<T> s = new ScheduleCron<>();
		s.cron = new CronAdjuster(cronExpression);
		s.job = job;
		s.env = type != null && type != Object.class
				? Converter.cnv(type, s.cron.getEnv()) : null;
		schedule(s, s.cron.isReboot() ? 1 : s.next());
		return () -> {
			s.canceled = true;
			s.promise.cancel();
		};
	}

	@Override
	public CancellablePromiseImpl<Instant> at(long epochTime) {
		long delay = epochTime - System.currentTimeMillis();
		return after(delay);
	}

	@Override
	public <T> CancellablePromiseImpl<T> at(Callable<T> callable,
			long epochTime) {
		long delay = epochTime - System.currentTimeMillis();
		return after(callable, delay);
	}

	class Cron<T> {

		CronJob<T>	target;
		Closeable	schedule;

		Cron(Class<T> type, CronJob<T> target, String cronExpression)
				throws Exception {
			this.target = target;
			schedule = schedule(type, target, cronExpression);
		}

		void close() throws IOException {
			schedule.close();
		}
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
	<T> void addSchedule(CronJob<T> s, Map<String, Object> map)
			throws Exception {
		String[] schedules = Converter.cnv(String[].class,
				map.get(CronJob.CRON));
		if (schedules == null || schedules.length == 0)
			return;

		Class<T> type = getType(s);

		synchronized (crons) {
			for (String schedule : schedules) {
				try {
					Cron<T> cron = new Cron<>(type, s, schedule);
					crons.add(cron);
				} catch (Exception e) {
					logger.error("Invalid  cron expression " + schedule
							+ " from " + map, e);
				}
			}
		}
	}

	void removeSchedule(CronJob<?> s) {
		synchronized (crons) {
			for (Iterator<Cron<?>> cron = crons.iterator(); cron.hasNext();) {
				try {
					Cron<?> c = cron.next();
					if (c.target == s) {
						cron.remove();
						c.schedule.close();
					}
				} catch (IOException e) {
					// we're closing, so ignore any errors
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	<T> Class<T> getType(CronJob<T> cj) {
		for (java.lang.reflect.Type c : cj.getClass().getGenericInterfaces()) {
			if (c instanceof ParameterizedType) {
				if (((ParameterizedType) c).getRawType() == CronJob.class) {
					return (Class<T>) ((ParameterizedType) c)
							.getActualTypeArguments()[0];
				}
			}
		}
		return null;
	}

}
