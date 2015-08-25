package osgi.enroute.rest.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface provides the REST's request properties. This encompasses the
 * following properties:
 * <ul>
 * <li>The Http Servlet Request
 * <li>The Http Servlet Response
 * <li>The Host name
 * <li>The body of the request
 * <li>Any parameters in the request, converted to types requested by the client
 * of this code
 * </ul>
 * <p>
 * Parameters in the request (e.g. {@code http:/url.com?abc=3&abc=4}) can be
 * added to this extended interface. For example, for the {@code abc} parameter
 * in the previous example one can add a {@code int[] abc()} method to the
 * extended interface. If the parameter is not present, null is returned or 0 if
 * it is a Number.
 * 
 */

@ProviderType
public interface RESTRequest {
	/**
	 * Provide access to the Http Servlet Request
	 * 
	 * @return The servlet request
	 */
	HttpServletRequest _request();

	/**
	 * Provide access to the Http Servlet Response
	 * 
	 * @return The servlet response
	 */
	HttpServletResponse _response();

	/**
	 * Provide access to the host name (of this computer)
	 * 
	 * @return The hostname
	 */
	String _host();

	/**
	 * Extend this interface with with no-arg methods whose name is the name of
	 * a parameter.
	 */
}
