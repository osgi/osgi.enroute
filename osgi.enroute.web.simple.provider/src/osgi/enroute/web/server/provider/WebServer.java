package osgi.enroute.web.server.provider;

import java.io.*;
import java.net.*;

import javax.servlet.http.*;

import org.osgi.framework.*;
import org.osgi.namespace.extender.*;
import org.osgi.service.component.annotations.*;
import org.osgi.service.log.*;
import org.osgi.util.tracker.*;

import aQute.bnd.annotation.headers.*;
import osgi.enroute.http.capabilities.*;
import osgi.enroute.servlet.api.*;
import osgi.enroute.web.server.cache.*;
import osgi.enroute.web.server.config.*;
import osgi.enroute.web.server.exceptions.*;
import osgi.enroute.webserver.capabilities.*;

@ProvideCapability(
		ns = ExtenderNamespace.EXTENDER_NAMESPACE, 
		name = WebServerConstants.WEB_SERVER_EXTENDER_NAME, 
		version = WebServerConstants.WEB_SERVER_EXTENDER_VERSION)
@RequireHttpImplementation
@Component(
		service = { ConditionalServlet.class }, 
		immediate = true, 
		property = {
				"service.ranking:Integer=1000", 
				"name=" + WebServer.NAME}, 
		name = WebServer.NAME, 
		configurationPid = WebServer.NAME,
		configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class WebServer implements ConditionalServlet {

	public static final String NAME = "osgi.enroute.simple.server";

	WebServerConfig						config;
	BundleTracker< ? >					tracker;
	Cache					cache;
	private ResponseWriter						writer;
	private ExceptionHandler			exceptionHandler;
	LogService				log;

	@Activate
	void activate(WebServerConfig config, BundleContext context) throws Exception {
		this.config = config;
		writer = new ResponseWriter(config);
		exceptionHandler = new ExceptionHandler(log);

		tracker = new BundleTracker<Bundle>(context, Bundle.ACTIVE | Bundle.STARTING, null) {
			public Bundle addingBundle(Bundle bundle, BundleEvent event) {
				if (bundle.getEntryPaths("static/") != null)
					return bundle;
				return null;
			}
		};
		tracker.open();
	}

	@Override
	public boolean doConditionalService(HttpServletRequest rq, HttpServletResponse rsp) throws Exception {
		try {
			String path = rq.getRequestURI();
			if (path != null && path.startsWith("/"))
				path = path.substring(1);

			FileCache c = getCache(path);
			if(c == null)
				return false;

			writer.writeResponse(rq, rsp, c);
		}
		catch (Exception e ) {
			exceptionHandler.handle(rq, rsp, e);
		}

		return true;
	}

	FileCache getCache(String path) throws Exception {
		FileCache c;
		cache.lock();
		try {
			c = cache.getFromCache(path);
			if (c == null || c.isExpired()) {
				c = findBundle(path);
				if (c == null) {
					c = do404(path);
				} else
					cache.putToCache(path, c);
			}
		} finally {
			cache.unlock();
		}
		return c;
	}

	private FileCache do404(String path) throws Exception {
		log.log(LogService.LOG_INFO, "404 " + path);
		FileCache c = findBundle("404.html");
		if (c == null)
			c = findBundle("default/404.html");
		if (c != null)
			c.is404 = true;

		return c;
	}

	FileCache findBundle(String path) throws Exception {
		Bundle[] bundles = tracker.getBundles();
		if (bundles != null) {
			for (Bundle b : bundles) {
				URL url = cache.urlOf(b, path);
				FileCache c = cache.getFromBundle(b, url, path);
				if(c != null)
					return c;
			}
		}
		return null;
	}

	//-------------- PLUGIN-CACHE --------------
	public File getFile(String path) throws Exception {
		FileCache c = getCache(path);
		if (c == null)
			return null;

		if (!c.isSynched())
			return null;

		return c.file;
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