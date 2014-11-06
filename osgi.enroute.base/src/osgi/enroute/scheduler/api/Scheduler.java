package osgi.enroute.scheduler.api;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

/**
 * A Scheduler service provides timed semantics to Promises. A Scheduler can
 * delay a promise, it can resolve a promise at a certain time, or it can
 * provide a timeout to a promise.
 */
public interface Scheduler {

	/**
	 * Execute a callable after a number of milliseconds. This promise will fail
	 * with a ShutdownException if this scheduler is closed.
	 * 
	 * @param callable
	 *            The callable to call
	 * @param ms
	 *            Number of milliseconds to delay
	 * @return a Promise that is resolved after the callable has returned.
	 */

	<T> Promise<T> delay(Callable<T> callable, long ms);

	default <T> Promise<T> delay(Callable<T> callable, Duration duration) {
		return delay(callable, duration.toMillis());
	}

	default <T> Promise<T> at(Callable<T> callable, Instant instant) {
		return delay(callable, ChronoUnit.MILLIS.between(instant, Instant.now()));
	}

	/**
	 * Return a promise that will resolve in the given number of milliseconds
	 * with the given value. This promise will fail with a ShutdownException if
	 * this scheduler is closed.
	 * 
	 * @param value
	 *            The value to resolve with
	 * @param ms
	 *            The number of milliseconds to delay
	 * @return a Promise that will be resolve with the given value after
	 *         milliseconds.
	 */
	default <T> Promise<T> delay(T value, long ms) {
		return delay(() -> value, ms);
	}

	default <T> Promise<T> delay(T value, Duration duration) {
		return delay(() -> value, duration.toMillis());
	}

	/**
	 * Return a promise that will resolve at the given epoch time. This promise
	 * will fail with a ShutdownException if this scheduler is closed.
	 * 
	 * @param value
	 *            The value to resolve with
	 * @param epochTimeInMs
	 *            the epoch time
	 * @return a promise that will be resolve at the given epoch time
	 */
	default <T> Promise<T> at(T value, long epochTimeInMs) {
		return delay(value, epochTimeInMs - System.currentTimeMillis());
	}

	/**
	 * Return a promise that will resolve at the given epoch time with the value
	 * that is returned from the callable. This promise will fail with a
	 * ShutdownException if this scheduler is closed.
	 * 
	 * @param callable
	 *            The value to resolve with
	 * @param epochTimeInMs
	 *            the epoch time
	 * @return a promise that will be resolve at the given epoch time
	 */
	default <T> Promise<T> at(Callable<T> callable, long epochTimeInMs) {
		return delay(callable, epochTimeInMs - System.currentTimeMillis());
	}

	/**
	 * Return a (Curried) function that returns a {@link Success} object. If the
	 * {@link Success#call(Promise)} method is called, this will return a new
	 * Promise that is delayed for the given number of milliseconds.
	 * 
	 * @param ms
	 *            Number of milliseconds to delay
	 * @return a function that will return a delayed promise
	 */

	default <T> Success<T,T> delay(long ms) {
		return (p) -> delay(p.getValue(), ms);
	}

	default <T> Success<T,T> delay(Duration duration) {
		return (p) -> delay(p.getValue(), duration);
	}

	static class Unique {
		AtomicBoolean	done	= new AtomicBoolean();

		interface RunnableException {
			public void run() throws Exception;
		}

		void once(RunnableException o) throws Exception {
			if (done.getAndSet(true) == false)
				o.run();
		}
	}

	/**
	 * Return a new Promise that will fail after timeout ms with a
	 * {@link TimeoutException}
	 */
	default <T> Promise<T> before(Promise<T> promise, long timeout) {
		Deferred<T> d = new Deferred<T>();
		Unique only = new Unique();

		delay(() -> {
			only.once(() -> d.fail(new TimeoutException()));
			return null;
		}, timeout);

		promise.then((p) -> {
			only.once(() -> d.resolve(p.getValue()));
			return null;
		}, (p) -> {
			only.once(() -> d.fail(p.getFailure()));
		});
		return d.getPromise();
	}

	default <T> Promise<T> after(Promise<T> promise, long timeout) {
		return after(promise, Instant.now().plusMillis(timeout));
	}

	default <T> Promise<T> after(Promise<T> promise, Instant instant) {
		Deferred<T> d = new Deferred<T>();

		promise.then((p) -> {
			long duration = Duration.between(Instant.now(), instant).toMillis();
			if (duration > 0) {
				delay( ()-> { 
					d.resolve(p.getValue()); 
					return null;
				}, duration);
			} else
				d.resolve(p.getValue());
			return null;
		}, (p)->{
			d.fail(p.getFailure());
		});
		return d.getPromise();
	}

	default Closeable schedule(Runnable r, TemporalAdjuster tj) {
		return null;
	}
}
