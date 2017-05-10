package osgi.enroute.web.server.provider;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;

import osgi.enroute.web.server.cache.Cache;
import osgi.enroute.web.server.cache.CacheFile;
import osgi.enroute.web.server.config.WebServerConfig;
import osgi.enroute.web.server.exceptions.ExceptionHandler;
import osgi.enroute.web.server.exceptions.NotFound404Exception;

@Component(
		name = "osgi.enroute.web.service.provider.bfs",
		property = {
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/bnd/*", 
				Constants.SERVICE_RANKING + ":Integer=100",
				"addTrailingSlash=true"
		}, 
		service = Servlet.class, 
		configurationPid = BundleMixinServer.NAME,
		configurationPolicy = ConfigurationPolicy.OPTIONAL,
		immediate = true)
public class BundleFileServer extends HttpServlet {

	private static final long					serialVersionUID	= 1L;

	protected WebServerConfig					config;
	private BundleTracker<?>					tracker;
	protected Map<String, Bundle>				bundles = new HashMap<>();
	private Cache								cache;
	private ResponseWriter						writer;
	private ExceptionHandler					exceptionHandler;
	private LogService							log;

	@Activate
	void activate(WebServerConfig config, BundleContext context) throws Exception {
		this.config = config;
		writer = new ResponseWriter(config);
		exceptionHandler = new ExceptionHandler(config.addTrailingSlash(), log);

		tracker = new BundleTracker<Bundle>(context, Bundle.ACTIVE | Bundle.STARTING, null) {
			public Bundle addingBundle(Bundle bundle, BundleEvent event) {
				String bsn = bundle.getSymbolicName();
				boolean found = false;
				if (config.debug())
					found = bundle.findEntries("static/debug/" + bsn, "*", false) != null;
				else
					found = bundle.findEntries("static/" + bsn, "*", false) != null;

				if(!found)
					return null;

				bundles.put(bsn, bundle);
				return bundle;
			}

			@Override
			public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
				bundles.remove(bundle.getSymbolicName());
			}
		};

		tracker.open();
	}

	public void doGet(HttpServletRequest rq, HttpServletResponse rsp) throws ServletException, IOException {

		Bundle b = null;
		String bsn = null;

		try {
			String path = rq.getPathInfo();

			if (path == null ) {
				throw new NotFound404Exception(bsn);
			}

			if (path.startsWith("/"))
				path = path.substring(1);

			if (path.contains("/"))
				bsn = path.substring(0, path.indexOf('/'));
			else
				bsn = path;

			b = bundles.get(bsn);
			if (b == null) {
				throw new NotFound404Exception(bsn);
			}

			boolean is404 = false;
			URL url = cache.internalUrlOf(b, path);
			if (url == null ) {
				// Attempt to load the 404.html file within the bundle
				path = bsn + "/404.html";
				is404 = true;
				url = cache.internalUrlOf(b, path);
				if (url == null )
					throw new NotFound404Exception(bsn);
			}

			CacheFile c = cache.getFromBundle(b, url, path);
			c.is404 = is404;
			cache.put(path, c);
			writer.writeResponse(rq, rsp, c);
		} catch (Exception e ) {
			exceptionHandler.handle(rq, rsp, e);
		}
	}

	@Deactivate
	void deactivate() {
		tracker.close();
	}

	@Reference
	void setLog(LogService log) {
		this.log = log;
	}

	@Reference
	void setCache(Cache cache) {
		this.cache = cache;
	}
}