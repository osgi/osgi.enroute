package osgi.enroute.websecurity.adapter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.metatype.Meta;
import aQute.lib.base64.Base64;
import aQute.lib.collections.ExtList;
import osgi.enroute.authentication.api.Authenticator;
import osgi.enroute.authorization.api.AuthorityAdmin;
import osgi.enroute.http.capabilities.RequireHttpImplementation;

@RequireHttpImplementation
@Designate(ocd=SecurityFilter.Config.class,factory=true)
@Component(property = {
	HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX+"=.*"
})
public class SecurityFilter implements Filter {
	final static String							DEFAULT_REALM		= "OSGi enRoute Default";
	private static final String					AUTH_PREFIX_BASIC	= "Basic ";

	static Logger								logger				= LoggerFactory.getLogger(SecurityFilter.class);
	private CopyOnWriteArrayList<Authenticator>	authenticators		= new CopyOnWriteArrayList<Authenticator>();
	private AtomicReference<AuthorityAdmin>		authorityAdminRef	= new AtomicReference<AuthorityAdmin>();
	private volatile boolean					reported;
	private String								realm;

	@ObjectClassDefinition
	@interface Config {
		@Meta.AD(deflt = DEFAULT_REALM)
		String realm();

		int service_ranking();

		String filter();

		String pattern();
		
		String osgi_http_whiteboard_filter_regex();
	}

	/*
	 * Configure this filter.
	 */
	@Activate
	void activate(Config config) {
		this.realm = config.realm();
	}

	@Override
	public void doFilter(final ServletRequest req, final ServletResponse resp, final FilterChain chain)
			throws IOException, ServletException {

		//
		// Create a lambda for our task to do
		//

		Callable<Void> runAs = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				chain.doFilter(req, resp);
				return null;
			}
		};

		//
		// We require that any authentication has a secure link.
		// Many authentication methods require confidentiality
		// and an open line is basically all,well, eh, open
		//

		if (!req.isSecure())
			run(null, runAs);

		if (req instanceof HttpServletRequest) {

			HttpServletRequest hreq = (HttpServletRequest) req;

			//
			// If we have a session, we do not lookup again.
			//
			String userId = null;
			if (hreq.getSession() != null) {
				userId = (String) hreq.getSession().getAttribute("userid");
			}

			if (userId == null)
				userId = authenticate(hreq);

			if (userId != null) {

				if (hreq.getSession() != null)
					hreq.getSession().setAttribute("userid", userId);

				run(userId, runAs);
				return;
			}
		}

		//
		// Report a 401. Should return with a properly authenticated
		// request
		//

		HttpServletResponse response = (HttpServletResponse) resp;
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setHeader("WWW-Authenticate", AUTH_PREFIX_BASIC + "realm=\"" + realm + "\"");
	}

	private void run(String userId, Callable<Void> runAs) throws ServletException, IOException {
		try {
			authorityAdminRef.get().call(userId, runAs);
		}
		catch (RuntimeException | ServletException | IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ServletException(e);
		}
	}

	private String authenticate(HttpServletRequest req) throws ServletException, IOException {
		Map<String,Object> map = getMap(req);

		for (Authenticator a : authenticators) {
			try {
				String user = a.authenticate(map, Authenticator.BASIC_SOURCE, Authenticator.SERVLET_SOURCE);
				if (user != null)
					return user;
			}
			catch (Exception e) {
				logger.error("Authenticator failed " + a, e);
			}
		}

		if (authenticators.isEmpty() && !reported) {
			logger.warn("There are no Authenticator services found ");
			reported = true;
		}

		return null;
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {}

	/**
	 * Turn a HttpServletRequest into a map for the authenticator according to
	 * the {@link Authenticator} service.
	 * 
	 * @param req
	 *            The (Http)ServletRequest
	 * @return a map
	 * @throws MalformedURLException
	 */
	private Map<String,Object> getMap(final HttpServletRequest req) throws MalformedURLException {

		Map<String,Object> map = new HashMap<String,Object>();
		String authHeader = req.getHeader("Authorization");
		if (authHeader != null) {

			authHeader = authHeader.substring(AUTH_PREFIX_BASIC.length());

			String decoded = new String(Base64.decodeBase64(authHeader));
			String[] userAndPass = decoded.split(":");
			if (userAndPass.length == 2) {
				map.put(Authenticator.BASIC_SOURCE_USERID, userAndPass[0]);
				map.put(Authenticator.BASIC_SOURCE_PASSWORD, userAndPass[1].toCharArray());
			}
		}

		/**
		 * A servlet.source will get a map that contains:
		 * <ul>
		 * <li>Under servlet.source, a URL object that represents the external
		 * request.
		 * <li>Under servlet.source.method, the request method.
		 * <li>All parameters. Multiple occurrence parameters are stored in a
		 * List, otherwise a string.
		 * <li>All servlet request headers</li> If a header as the same name as
		 * a parameter, then the parameter overrides the header.
		 */

		if (req instanceof HttpServletRequest) {
			HttpServletRequest hreq = (HttpServletRequest) req;
			for (Enumeration<String> e = hreq.getHeaderNames(); e.hasMoreElements();) {
				String key = e.nextElement();
				String header = hreq.getHeader(key);
				map.put(key, header);
			}
			map.put("servlet.source", new URL(hreq.getRequestURL().toString()));
			map.put("servlet.source.method", hreq.getMethod());
			map.put("servlet.secure", hreq.isSecure());
		}
		for (String key : req.getParameterMap().keySet()) {
			String[] parameterValues = req.getParameterValues((String) key);
			if (parameterValues != null) {
				if (parameterValues.length > 1)
					map.put(key, new ExtList<String>(parameterValues));
				else
					map.put(key, parameterValues[0]);
			}
		}

		return map;
	}

	@Override
	public void destroy() {}

	@Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
	void addAuthenticator(Authenticator authenticator) {
		authenticators.add(authenticator);
		reported = false;
	}

	void removeAuthenticator(Authenticator authenticator) {
		authenticators.remove(authenticator);
	}

	@Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
	void setAuthorityAdmin(AuthorityAdmin authorityAdmin) {
		authorityAdminRef.set(authorityAdmin);
	}

	void unsetAuthorityAdmin(AuthorityAdmin authorityAdmin) {
		authorityAdminRef.compareAndSet(authorityAdmin, null);
	}
}
