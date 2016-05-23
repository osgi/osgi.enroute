package osgi.enroute.web.server.provider;

import java.util.*;

import javax.servlet.http.*;

import org.osgi.framework.*;
import org.osgi.service.component.annotations.*;

import osgi.enroute.servlet.api.*;

@Component(
		service = { ConditionalServlet.class }, 
		immediate = true, 
		property = {
				"service.ranking:Integer=999", 
				"name=" + RedirectServlet.NAME, 
				// What the heck is this???? Can't find any doc about it.
				"no.index=true"
		}, 
		name = RedirectServlet.NAME, 
		configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class RedirectServlet implements ConditionalServlet {

	static final String NAME = "osgi.enroute.simple.redirect";

	/**
	 * Must start with a "/".
	 */
	private String						redirect	= "/index.html";

	@interface Config {
		String redirect();
	}

	@Activate
	void activate(Config config, Map<String,Object> props, BundleContext context) throws Exception {
		if (config.redirect() != null)
			redirect = config.redirect();

		if (!redirect.startsWith("/"))
			redirect = "/" + redirect;
	}

	@Override
	public boolean doConditionalService(HttpServletRequest rq, HttpServletResponse rsp) throws Exception {
		// Redirect is disabled by configuring with an empty string.
		// Since the value will be prepended with "/", it means that when the value is "/", no action is taken.
		if ("/".equals(redirect))
			return false;

		try {
			String path = rq.getRequestURI();

			if (path == null || path.isEmpty() || path.equals("/")) {
				throw new RedirectException(redirect);
			} else if (path.startsWith("/"))
				path = path.substring(1);

			if (path.endsWith("/")) {
				if (path.endsWith("/"))
					path = path.substring(0, path.length() - 1);
				throw new RedirectException("/" + path + redirect);
			}

			return false;
		}
		catch (RedirectException e) {
			rsp.sendRedirect(e.getPath());
			return true;
		}
	}
}
