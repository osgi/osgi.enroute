package osgi.enroute.rest.simple.provider;

import java.io.FileNotFoundException;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletResponse;

/**
 * Maintains a list of exceptions and the response code that such instances (or
 * from subclasses) map to. Since we take subclasses into account it is crucial
 * to properly order the list. The order should reflect the order in exception
 * catching.
 */
class ResponseException {

	final private Class<? extends Throwable>	throwable;
	final private int							statusCode;

	private ResponseException(Class<? extends Throwable> throwable,
			int statusCode) {
		this.throwable = throwable;
		this.statusCode = statusCode;
	}

	static ResponseException[] MATCH = {
            new ResponseException(FileNotFoundException.class,
                    HttpServletResponse.SC_NOT_FOUND),
            new ResponseException(NoSuchMethodException.class,
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED),
			new ResponseException(SecurityException.class,
					HttpServletResponse.SC_FORBIDDEN),
			new ResponseException(UnsupportedOperationException.class,
					HttpServletResponse.SC_NOT_IMPLEMENTED),
			new ResponseException(IllegalArgumentException.class,
					HttpServletResponse.SC_BAD_REQUEST),
			new ResponseException(LoginException.class,
					HttpServletResponse.SC_UNAUTHORIZED) };

	public static int getStatusCode(Class<? extends Throwable> exception,
			int defltCode) {
		for (ResponseException re : MATCH) {
			if (re.throwable.isAssignableFrom(exception))
				return re.statusCode;
		}
		return defltCode;
	}

	public static int getStatusCode(Class<? extends Throwable> class1) {
		return getStatusCode(class1,
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}
}
