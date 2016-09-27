package osgi.enroute.rest.api;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * A marker interface that allows a REST provider to call this class through
 * this interface. All the methods in the implementation class that have as
 * first argument an interface that extends {@link RESTRequest} become available
 * from the web. The actual URI depends on the {@code osgi.enroute.rest:rest}
 * configuration. By default this is {@code /rest}.
 * <p>
 * Incoming requests are used to construct a method name. The first part of the
 * method is the HTTP verb. That is, {@code GET, PUT, POST, DELETE, OPTION},
 * etc. The second part is the next <i>segment</i> in the URI. This name is then
 * compared case insensitive with any public methods that accept a
 * {@link RESTRequest} in their first argument.
 * <p>
 * The following segments, if any, are mapped to the arguments of the method. 
 * 
 *  
 *  
 *  The name of the method defines
 * the path. The method name is broken by on the first upper case letter. The
 * first part is the HTTP verb: {@code get, put, delete, option, post}. The
 * remaining parts are the URI relative from the REST base URI. I.e.
 * {@code getFoo} maps to {@code http://host.com/rest/foo/bar}, assuming that
 * the REST endpoint is {@code http://host.com/rest}.
 * <p>
 * The first argument extends {@link RESTRequest} and thereby provides access to
 * the request information.
 * <p>
 * It must be realized that any of the web request methods is called from
 * outside the system and requires proper security checks.
 */

@ConsumerType
public interface REST {
	/**
	 * Service property specify the endpoint prefix. The default is /rest
	 */
	String ENDPOINT = "endpoint";
}
