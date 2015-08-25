package osgi.enroute.scheduler.api;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;

import org.osgi.util.promise.Promise;

import osgi.enroute.dto.api.DTOs;

/**
 * A Scheduler service provides timed semantics to Promises. A Scheduler can
 * delay a promise, it can resolve a promise at a certain time, or it can
 * provide a timeout to a promise.
 * <p>
 * This scheduler has a millisecond resolution.
 */
public interface Scheduler {
	/**
	 * Convenience interface that is a Runnable but allows exceptions
	 */
	interface RunnableWithException {
		void run() throws Exception;
	}

	/**
	 * Return a promise that will resolve after the given number of
	 * milliseconds. This promise can be canceled.
	 * 
	 * @param ms
	 *            Number of milliseconds to delay
	 * @return A cancellable Promise
	 */
	CancellablePromise<Instant> after(long ms);

	/**
	 * Return a promise that resolves after delaying ms with the result of the
	 * call that is executed after the delay.
	 * 
	 * @param call
	 *            provides the result
	 * @param ms
	 *            Number of ms to delay
	 * @return A cancellable Promise
	 */
	<T> CancellablePromise<T> after(Callable<T> call, long ms);

	/**
	 * Return a promise that resolves at the given epochTime
	 * 
	 * @param epochTime
	 *            The Java (System.currentMillis) time
	 * @return A cancellable Promise
	 */
	CancellablePromise<Instant> at(long epochTime);

	/**
	 * Return a promise that resolves at the given epochTime with the result of
	 * the call.
	 * 
	 * @param callable
	 *            provides the result
	 * @param epochTime
	 *            The Java (System.currentMillis) time
	 * @return A cancellable Promise
	 */
	<T> CancellablePromise<T> at(Callable<T> callable, long epochTime);

	/**
	 * Schedule a runnable to be executed for the give cron expression (See
	 * {@link CronJob}). Every time when the cronExpression matches the current
	 * time, the runnable will be run. The method returns a closeable that can
	 * be used to stop scheduling. This variation does not take an environment
	 * object.
	 * 
	 * @param r
	 *            The Runnable to run
	 * @param cronExpression
	 *            A Cron Expression
	 * @return A closeable to terminate the schedule
	 * @throws Exception
	 */
	Closeable schedule(RunnableWithException r, String cronExpression) throws Exception;

	/**
	 * Schedule a runnable to be executed for the give cron expression (See
	 * {@link CronJob}). Every time when the cronExpression matches the current
	 * time, the runnable will be run. The method returns a closeable that can
	 * be used to stop scheduling. The run metjod of r takes an environment
	 * object. An environment object is a custom interface where the names of
	 * the methods are the keys in the properties (see {@link DTOs}).
	 * 
	 * @param type
	 *            The data type of the parameter for the cron job
	 * @param r
	 *            The Runnable to run
	 * @param cronExpression
	 *            A Cron Expression
	 * @return A closeable to terminate the schedule
	 * @throws Exception
	 */
	<T> Closeable schedule(Class<T> type, CronJob<T> r, String cronExpression) throws Exception;
	
	/**
	 * Schedule a runnable to be executed in a loop. The first time the first is
	 * as delay, later the values in ms are used sequentially. If no more values
	 * are present, the last value is re-used. The method returns a closeable
	 * that can be used to stop scheduling. This is a fixed rate scheduler. That
	 * is, a base time is established when this method is called and subsequent
	 * firings are always calculated relative to this start time.
	 * 
	 * @param r
	 *            The Runnable to run
	 * @param first
	 *            The first time to use
	 * @param ms
	 *            The subsequent times to use.
	 * @return A closeable to terminate the schedule
	 * @throws Exception
	 */
	Closeable schedule(RunnableWithException r, long first, long... ms) throws Exception;

	/**
	 * Return a cancellable promise that fails with a {@link TimeoutException}
	 * when the given promise is not resolved before the given timeout. If the
	 * given promise fails or is resolved before the timeout then the returned
	 * promise will be treated accordingly. The cancelation does not influence
	 * the final result of the given promise since a Promise can only be failed
	 * or resolved by its creator.
	 * <p>
	 * If the timeout is in the past then the promised will be resolved
	 * immediately
	 * 
	 * @param promise
	 *            The promise to base the returned promise on
	 * @param timeout
	 *            The number of milliseconds to wait.
	 * @return A cancellable Promise
	 */
	<T> CancellablePromise<T> before(Promise<T> promise, long timeout);

	/**
	 * Convenience method to use an Instant. See {@link #at(long)}
	 * 
	 * @param instant
	 *            The instant for the time
	 * @return a cancellable promise that is resolved when the instant has been
	 *         reached
	 */
	default CancellablePromise<Instant> at(Instant instant) {
		return at(instant.toEpochMilli());
	}

	/**
	 * Convenience method to use an instant and a Callable. See
	 * {@link #at(Callable, long)}.
	 * 
	 * @param callable
	 *            will be called when instant is reached
	 * @param instant
	 * @return a cancellable promise that is resolved when the instant has been
	 *         reached and the callable has been called
	 */
	default <T> CancellablePromise<T> at(Callable<T> callable, Instant instant) {
		return at(callable, instant.toEpochMilli());
	}

	/**
	 * Convenience method to use an instant and a RunnableWithException. See
	 * {@link #at(RunnableWithException, long)}.
	 * 
	 * @param r
	 *            the runnable with exception to call when instant has been
	 *            reached
	 * @param instant
	 *            the time to run r
	 * @return A cancellable promise
	 */
	default CancellablePromise<Void> at(RunnableWithException r, Instant instant) {
		return at(r, instant.toEpochMilli());
	}

	/**
	 * Convenience method to use an instant and a RunnableWithException. See
	 * {@link #at(RunnableWithException, long)}.
	 * 
	 * @param r
	 *            the runnable with exception to call when instant has been
	 *            reached
	 * @param epochMilli
	 *            the time at which to run in epoch time
	 * @return A cancellable promise
	 */
	default CancellablePromise<Void> at(RunnableWithException r, long epochMilli) {
		return at(() -> {
			r.run();
			return null;
		}, epochMilli);
	}

	/**
	 * Convenience method to use a Duration instead of a millisecond delay. See
	 * {@link #after(long)}.
	 * 
	 * @param d
	 *            the duration to wait
	 * @return A cancellable promise
	 */
	default CancellablePromise<Instant> after(Duration d) {
		return after(d.toMillis());
	}

	/**
	 * Convenience method to to a duration and a callable. See
	 * {@link #after(Callable, long)}.
	 * 
	 * @param call
	 *            the callable to call
	 * @param d
	 *            the duration to wait
	 * @return A cancellable promise
	 */
	default <T> CancellablePromise<T> after(Callable<T> call, Duration d) {
		return after(call, d.toMillis());
	}

	/**
	 * Convenience method to to a duration and a RunnableWithException. See
	 * {@link #after(Callable, long)}.
	 * 
	 * @param r
	 *            the runnable with exception to call when instant has been
	 *            reached
	 * @param ms
	 *            the time to wait in milliseconds
	 * @return A cancellable promise
	 */
	default <T> CancellablePromise<T> after(RunnableWithException r, long ms) {
		return after(() -> {
			r.run();
			return null;
		}, ms);
	}

	/**
	 * Convenience method to use durations instead of milliseconds. See
	 * {@link #schedule(RunnableWithException, long, long...)}
	 * 
	 * @param r
	 *            the runnable to run after each duration
	 * @param first
	 *            the first duration
	 * @param duration
	 *            subsequent durations
	 * @return A cancellable promise
	 * @throws Exception
	 */
	default Closeable schedule(RunnableWithException r, Duration first, Duration... duration) throws Exception {
		long[] ms = new long[duration.length];
		for (int i = 0; i < ms.length; i++) {
			ms[i] = duration[i].toMillis();
		}
		return schedule(r, first.toMillis(), ms);
	}

}
