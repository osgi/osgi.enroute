package osgi.enroute.authentication.api;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import osgi.enroute.authorization.api.Authority;

/**
 * An authenticator is a service that can provide an authenticated id.
 * <p>
 * In general, the credentials for an authentication are provided by the user
 * interface, which is quite often in a completely different subsystem. E.g.
 * with a web based UI the credentials are coming from the Javascript app. The
 * actual code that must handle the login process, and thus authenticate, should
 * preferably not be aware of this agreement between the UI and the
 * authenticator.
 * <p>
 * The idea behind this authenticator is that it acts as a conduit between the
 * GUI (wherever it is, and the actual authenticator used. This implies that the
 * interface is quite generic and bland.
 * <p>
 * An authenticator (Mozilla Persona, JAAS login modules, etc) must register an
 * Authenticator service. A party that needs to authenticate, a servlet filter
 * for example, must map the parameters it has (i.e. id/password, browser id,
 * signed token) to a Map. This map is then passed to the authenticator. The
 * source type can specify what the receiver can expect in the map. One source
 * type is the {@link #SERVLET_SOURCE} type, this states that the map must
 * contain all parameters and all headers of a servlet request.
 * <p>
 * The user id is a unique identifier for the local system. The local User Admin
 * can be used to get more information for that user, the key '_id' is assumed
 * to be set to this id. In general, the id should be an email address.
 * <p>
 * In general, security model is that a Servlet Filter handles the login details
 * and then uses the {@link Authority} service to establish a current user (with
 * a current set of roles) as the default user for a thread.
 * <p>
 * How to use this service:
 * 
 * <pre>
 * public void doFilter(ServletRequest rq, ServletResponse rsp, FilterChain
 * downstream) throws IOException { ... String id = null; Map<String,Object> map
 * = makeMap(rq); for ( Authenticator authenticator : authenticators ) { id =
 * authenticator.authenticate(map,SERVLET_SOURCE, BASIC_SOURCE); if ( id != null
 * ) { authority.call(id, new Callable<Void>() { public Void call() throws
 * Exception { chain.doFilter(rq,rsp); } }); return; } } .... Map
 * <String,Object> makeMap(ServletRequest rq) { // turn the request into a map }
 * }
 */
@ProviderType
public interface Authenticator {

	/**
	 * A servlet.source will get a map that contains:
	 * <ul>
	 * <li>Under servlet.source, a URL object that represents the external
	 * request.
	 * <li>Under servlet.source.method, the request method.
	 * <li>All parameters. Multiple occurrence parameters are stored in a List,
	 * otherwise a string.
	 * <li>All servlet request headers</li> If a header as the same name as a
	 * parameter, then the parameter overrides the header.
	 */

	String SERVLET_SOURCE = "servlet.source";

	/**
	 * The property name for the request method
	 */
	String SERVLET_SOURCE_METHOD = "servlet.source.method";

	/**
	 * If only a user id and password are required. The
	 * {@link #BASIC_SOURCE_USERID} property must be set to the user id. The
	 * {@link #BASIC_SOURCE_PASSWORD} must be set to the password. Web logins
	 * should also set these sources but should take into account that basic
	 * authentication only works with a confidential connection.
	 */
	String BASIC_SOURCE = "basic.source";

	/**
	 * The property key for a password with the {@link #BASIC_SOURCE}.
	 */
	String BASIC_SOURCE_PASSWORD = "user.source.password";

	/**
	 * The property key for a userid with the {@link #BASIC_SOURCE}.
	 */
	String BASIC_SOURCE_USERID = "user.source.userid";

	/**
	 * Attempt to authenticate the caller based on he properties in @{code
	 * arguments}. The map is formatted according to the names specified in
	 * sources. If no sources are specified, the authenticator is free to
	 * inspect the map to see if it can find a way to authenticate a user.
	 * <p>
	 * If a user is authenticated, then the system wide unique id must be
	 * returned. If the authenticator cannot authenticate, it is must return
	 * null.
	 * 
	 * @param arguments
	 *            The properties from the request processor
	 * @param sources
	 *            Identifying keys for the types of the arguments
	 * @return A valid user id if authenticated or {@code null} if not
	 * @throws Exception
	 */
	String authenticate(Map<String,Object> arguments, String... sources) throws Exception;

	/**
	 * Remove any login information cached for the given user id. The next time
	 * this user id is authenticated, the authenticator must refresh the user
	 * information from its source. For example, if this authenticator is backed
	 * by an LDAP server that caches the 'roles' then these roles should be
	 * removed until it is authenticated again. This call will also remove any
	 * permissions the given user has.
	 * 
	 * @param userid
	 *            The user id to remove any cached information from
	 * @return <code>true</code> if there was cached information,
	 *         <code>false</code> otherwise
	 * @throws Exception
	 */
	boolean forget(String userid) throws Exception;
}
