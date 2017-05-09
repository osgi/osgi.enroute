package osgi.enroute.authorization.api;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Formatter;

/**
 * This class provides a convenient way to verify the permissions. It defines
 * the permissions in an interface and uses a proxy to test the method names
 * against the actual permissions.
 * <p>
 * If the method name contains a '_' then it is replaced with a '.'.
 */
public class SecurityVerifier {
	static final String[]	EMPTY_STRING	= new String[0];

	static class CheckHandler extends SecurityVerifier.HasHandler {

		CheckHandler(Authority authority) {
			super(authority);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			if (authority == null)
				return true;

			if (!has(method.getName(), args)) {
				Formatter f = new Formatter();
				if (args != null && args.length > 0) {
					String del = " with args: ";
					for (Object arg : args) {
						f.format("%s%s", del, arg);
						del = ", ";
					}
				}
				String extra = f.toString();
				f.close();

				throw new SecurityException("No permission for " + method.getName().replace('_', '.') + extra);
			}

			return true;
		}
	}

	static class HasHandler implements InvocationHandler {
		Authority	authority;

		public HasHandler(Authority authority) {
			this.authority = authority;
		}

		boolean has(String name, Object[] args) throws Exception {
			String[] args0;
			if (args == null || args.length == 0)
				args0 = EMPTY_STRING;
			else
				args0 = toStrings(args);

			return this.authority.hasPermission(name.replace('_', '.'), args0);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (authority == null)
				return true;

			return has(method.getName(), args);
		}

		private static String[] toStrings(Object[] args) {
			String[] args0 = new String[args.length];

			for (int i = 0; i < args.length; i++) {

				if (args[i] == null)
					continue;

				args0[i] = args[i].toString();
			}

			return args0;
		}

	}

	/**
	 * Create a security checker. This one will throw a Security Exception when
	 * the permission was not granted.
	 * 
	 * @param type
	 *            the interface with the method names as the permission names.
	 * @param authority
	 *            The Authority service
	 * @return A security exception throwing security checker based on the given
	 *         interface
	 */
	@SuppressWarnings("unchecked")
	public static <T> T createChecker(Class<T> type, Authority authority) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {
			type
		}, new CheckHandler(authority));
	}

	/**
	 * Create a security verifier. This one will throw a Security Exception when
	 * the permission was not granted.
	 * 
	 * @param type
	 *            the interface with the method names as the permission names.
	 * @param authority
	 *            The Authority service
	 * @return A security verifier that returns booleans instead of throwing an
	 *         exception
	 */
	@SuppressWarnings("unchecked")
	public static <T> T createVerifier(Class<T> type, Authority authority) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {
			type
		}, new HasHandler(authority));
	}
}
