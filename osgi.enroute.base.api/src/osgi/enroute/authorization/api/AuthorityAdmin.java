package osgi.enroute.authorization.api;

import java.util.concurrent.Callable;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A service that allows a call to proceed with a user associated with the
 * current thread.
 * <p>
 * This method is intended to be used by for example servlet filters that
 * authenticate the requester. The should then call the downstream filters and
 * servlet using the {@link #call(String, Callable)} method. The Authority Admin
 * service will then establish a context associated with the current thread. Any
 * callee downstream can then get the current user and the permissions
 * associated with the current user using the {@link Authority} service.
 */

@ProviderType
public interface AuthorityAdmin {
	/**
	 * Associate the current thread with the given user and call the
	 * protectedTask.
	 * 
	 * @param userId
	 *            Either an authenticated user or null. In the case of null, an
	 *            anonymous user is used.
	 * @param protectedTask
	 *            The task executed with the give userid as the current user
	 * @return The result of the protectedTask
	 * @throws Exception
	 */
	<T> T call(String userId, Callable<T> protectedTask) throws Exception;
}
