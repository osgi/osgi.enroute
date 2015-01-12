package osgi.enroute.scheduler.api;

import org.osgi.util.promise.Promise;

/**
 * A Cancellable Promise is a Promise that can be canceled. This will fail it
 * with the {@link CancelException}, which is a singleton.
 * 
 * @param <T>
 *            The promise type
 */
public interface CancellablePromise<T> extends Promise<T> {

	/**
	 * Cancel this promise (fail it with {@link CancelException}, which is a
	 * singleton).
	 * 
	 * @return true if this promise is canceled or false if it already was
	 *         canceled.
	 */
	boolean cancel();
}
