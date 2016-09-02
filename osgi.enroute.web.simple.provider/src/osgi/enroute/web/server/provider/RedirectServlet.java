package osgi.enroute.web.server.provider;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import osgi.enroute.servlet.api.ConditionalServlet;
import osgi.enroute.web.server.exceptions.Redirect302Exception;

@Component(
		service = { ConditionalServlet.class }, 
		immediate = true, 
		property = {
				"service.ranking:Integer=1000", 
				"name=" + RedirectServlet.NAME, 
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
				throw new Redirect302Exception(redirect);
			} else if (path.startsWith("/"))
				path = path.substring(1);

			if (path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
				throw new Redirect302Exception("/" + path + redirect);
			}

			return false;
		}
		catch (Redirect302Exception e) {
			rsp.setHeader("Location", e.getPath());
			rsp.sendRedirect(e.getPath());
			return true;
		}
	}
}
