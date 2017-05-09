package osgi.enroute.authorization.api;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The AuthorityAdmin service establishes a thread context for a current user.
 * This service allows access to this current user and its available
 * permissions.
 * <p>
 * Permissions are represented as strings. They map to the (misnomer) of role in
 * Java security discussions. A user can have many permissions. Strings are used
 * because this makes it possible to allow other parties, like Javascript, to
 * evaluate the permissions to adjust the UI or do pre-checks; Java permissions
 * make this impossible.
 * <p>
 * Permissions are {@code symbolic} (basically [-_.a-zA-Z0-9]). However, they
 * can contain a place holder for <em>arguments</em>. Arguments are filter
 * wildcard expressions on specified arguments separated by a semicolon ';'.
 * Arguments can use any character except control (<0x20, so also no cr/lf). In
 * the unlikely case you need to match an argument with a ';', use the Unicode
 * 'full width semicolon' (\uFF1B) instead.
 * 
 * <pre>
 * 	example.write.file
 *  example.write.file;/tmp/*
 * </pre>
 * 
 * Empty or missing places match any argument.
 */
@ProviderType
public interface Authority {
	/**
	 * Get the current user id.
	 * 
	 * @return the current user id. This is never <code>null</code>
	 * @throws Exception
	 */
	String getUserId() throws Exception;

	/**
	 * Get the set of permissions associated with the current user.
	 * 
	 * @return the set of permissions. If there are no permissions, an empty
	 *         list is returned. This list is read only.
	 * @throws Exception
	 */
	List<String> getPermissions() throws Exception;

	/**
	 * Verify if the current user has the given permission. First, a check is
	 * made to see if the current user has the given permission. If not, false
	 * is returned. Then, if the permission contains arguments (separated by
	 * ';') then each wildcard expression is verified against the given
	 * arguments. I a wildcard expression is empty it matches anything, even a
	 * missing argument or null, just like a full wildcard. Otherwise the
	 * expression must match the argument and the argument must be present.
	 * 
	 * @param permission
	 *            The permission to check
	 * @param arguments
	 *            The arguments specified in the permission
	 * @return true if the current user is granted the permission, otherwise
	 *         false
	 * @throws Exception
	 */
	boolean hasPermission(String permission, String... arguments) throws Exception;

	/**
	 * Call {@link #hasPermission(String, String...)}. If this returns true,
	 * this method returns silently. Otherwise it will throw a
	 * SecurityException.
	 * 
	 * @param permission
	 *            The permission to check
	 * @param arguments
	 *            The arguments to the check
	 * @throws Exception
	 * @throws SecurityException
	 *             if the permission is not granted to the current caller
	 */
	void checkPermission(String permission, String... arguments) throws Exception;
}
