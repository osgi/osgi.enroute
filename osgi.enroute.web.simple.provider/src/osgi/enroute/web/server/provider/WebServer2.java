package osgi.enroute.web.server.provider;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.text.*;
import java.util.*;

import javax.servlet.Filter;
import javax.servlet.http.*;

import org.osgi.framework.*;
import org.osgi.namespace.extender.*;
import org.osgi.service.component.annotations.*;
import org.osgi.service.coordinator.*;
import org.osgi.service.log.*;
import org.osgi.util.tracker.*;

import aQute.bnd.annotation.headers.*;
import aQute.lib.io.*;
import aQute.lib.json.*;
import aQute.libg.sed.*;
import osgi.enroute.http.capabilities.*;
import osgi.enroute.servlet.api.*;
import osgi.enroute.web.server.cache.*;
import osgi.enroute.web.server.config.*;
import osgi.enroute.web.server.exceptions.*;
import osgi.enroute.web.server.provider.IndexDTO.*;
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
				"service.ranking:Integer=1001", 
				"name=" + WebServer2.NAME, 
		}, 
		name = WebServer2.NAME, 
		configurationPolicy = 
		ConfigurationPolicy.OPTIONAL)
public class WebServer2 implements ConditionalServlet {

	static final String NAME = "osgi.enroute.simple.server2";

	static SimpleDateFormat	format							= new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",
			Locale.ENGLISH);
	LogService				log;
	boolean					proxy;
	PluginContributions		pluginContributions;
	WebResources			webResources;
	IndexDTO				index							= new IndexDTO();
	Cache					cache;
	BundleContext			context;

	WebServerConfig						config;
	private ServiceRegistration<Filter>	webfilter;
	private Coordinator					coordinator;
	private ServiceRegistration<Filter>	exceptionFilter;
	private BundleTracker<Bundle>		apps;
	private ExceptionHandler			exceptionHandler;

	@Activate
	void activate(WebServerConfig config, Map<String,Object> props, BundleContext context) throws Exception {
		this.context = context;
		index.configuration = props;
		this.config = config;
		this.exceptionHandler = new ExceptionHandler(log);
		proxy = !config.noproxy();

		pluginContributions = new PluginContributions(this, cache, context);
		webResources = new WebResources(this, cache, context);

		Hashtable<String,Object> p = new Hashtable<String,Object>();
		p.put("pattern", ".*");
		webfilter = context.registerService(Filter.class,
				new WebFilter(config.maxConnections(), config.maxConnectionMessage(), coordinator), p);

		apps = new BundleTracker<Bundle>(context, Bundle.ACTIVE, null) {
			@Override
			public Bundle addingBundle(Bundle bundle, BundleEvent event) {
				String app = bundle.getHeaders().get("EnRoute-Application");
				if (app == null)
					return null;

				String[] links = app.split("\\s*,\\s*");
				for (String link : links) {
					ApplicationDTO appdto = new ApplicationDTO();
					appdto.bsn = bundle.getSymbolicName();
					appdto.version = bundle.getHeaders().get(Constants.BUNDLE_VERSION);
					appdto.bundle = bundle.getBundleId();
					appdto.description = bundle.getHeaders().get(Constants.BUNDLE_DESCRIPTION);
					appdto.link = link;
					appdto.name = bundle.getHeaders().get(Constants.BUNDLE_NAME);
					if (appdto.name == null)
						appdto.name = appdto.bsn;

					synchronized (index) {
						index.applications.add(appdto);
					}
				}

				return super.addingBundle(bundle, event);
			}

			@Override
			public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
				synchronized (index) {
					for (Iterator<ApplicationDTO> i = index.applications.iterator(); i.hasNext();) {
						ApplicationDTO dto = i.next();
						if (dto.bundle == bundle.getBundleId())
							i.remove();
					}
				}
				super.removedBundle(bundle, event, object);
			}
		};
		apps.open();
	}

	@Override
	public boolean doConditionalService(HttpServletRequest rq, HttpServletResponse rsp) throws Exception {
		try {
			String path = rq.getRequestURI();
			if (path != null && path.startsWith("/"))
				path = path.substring(1);

			FileCache c = getCache(path);

			if (c == null || !c.isSynched()) {
				if ("index.html".equals(path)) {
					index(rsp);
					return true;
				} else {
					return false;
				}
			}

			rsp.setDateHeader("Last-Modified", c.time);
			rsp.setHeader("Etag", c.etag);
			rsp.setHeader("Content-MD5", c.md5);
			rsp.setHeader("Allow", "GET, HEAD");
			rsp.setHeader("Accept-Ranges", "bytes");

			long diff = 0;
			if (c.expiration != 0)
				diff = c.expiration - System.currentTimeMillis();
			else {
				diff = config.expiration();
				if (diff == 0)
					diff = 120000;
			}

			if (diff > 0) {
				rsp.setHeader("Cache-Control", "max-age=" + diff / 1000);
			}

			if (c.mime != null)
				rsp.setContentType(c.mime);

			Range range = new Range(rq.getHeader("Range"), c.file.length());
			long length = range.length();
			if (length >= Integer.MAX_VALUE)
				throw new IllegalArgumentException("Range to read is too high: " + length);

			rsp.setContentLength((int) range.length());

			if (config.expires() != 0) {
				Date expires = new Date(System.currentTimeMillis() + 60000 * config.expires());
				rsp.setHeader("Expires", format.format(expires));
			}

			String ifModifiedSince = rq.getHeader("If-Modified-Since");
			if (ifModifiedSince != null) {
				long time = 0;
				try {
					synchronized (format) {
						time = format.parse(ifModifiedSince).getTime();
					}
					if (time > c.time) {
						rsp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						return true;
					}
				}
				catch (Exception e) {
					// e.printStackTrace();
				}
			}

			String ifNoneMatch = rq.getHeader("If-None-Match");
			if (ifNoneMatch != null) {
				if (ifNoneMatch.indexOf(c.etag) >= 0) {
					rsp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					return true;
				}
			}

			if (rq.getMethod().equalsIgnoreCase("GET")) {

				rsp.setContentLengthLong(range.length());
				OutputStream out = rsp.getOutputStream();

				try (FileInputStream file = new FileInputStream(c.file);) {
					FileChannel from = file.getChannel();
					WritableByteChannel to = Channels.newChannel(out);
					range.copy(from, to);
					from.close();
					to.close();
				}

				out.flush();
				out.close();
				rsp.getOutputStream().flush();
				rsp.getOutputStream().close();
			}

			if (c.is404)
				return false;
			else
				rsp.setStatus(HttpServletResponse.SC_OK);

		}
		catch (Exception e) {
			exceptionHandler.handle(rq, rsp, e);
		}

		return true;
	}

	private void index(HttpServletResponse rsp) throws Exception {
		Bundle b = context.getBundle();
		URL url = cache.urlOf(b, "osgi/enroute/web/index.html");
		FileCache c = cache.getFromBundle(b, url, "osgi/enroute/web/index.html");
		if (c == null || c.is404 || c.isNotFound()) {
			url = cache.urlOf(b, "osgi/enroute/web/local/index.html");
			c = cache.getFromBundle(b, url, "osgi/enroute/web/local/index.html");
		}

		String content = IO.collect(c.file);
		Map<String,String> map = new HashMap<>();

		synchronized (index) {
			map.put("index", new JSONCodec().enc().put(index).indent(" ").toString());
		}

		ReplacerAdapter ra = new ReplacerAdapter(map);
		content = ra.process(content);
		IO.store(content, rsp.getOutputStream());
	}

	FileCache getCache(String path) throws Exception {
		FileCache c;
		cache.lock();
		try {
			c = cache.getFromCache(path);
			if (c == null || c.isExpired()) {
				c = find(path);
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
		FileCache c = find("404.html");
		if (c != null)
			c.is404 = true;

		return c;
	}

	FileCache find(String path) throws Exception {
		if (proxy && path.startsWith("$"))
			return cache.findCachedUrl(path);

		if (path.startsWith(PluginContributions.CONTRIBUTIONS + "/"))
			return pluginContributions
					.findCachedPlugins(path.substring(PluginContributions.CONTRIBUTIONS.length() + 1));

		FileCache c = webResources.find(path);

		return c;
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
		pluginContributions.close();
		if (webfilter != null)
			webfilter.unregister();
		if (exceptionFilter != null)
			exceptionFilter.unregister();

		apps.close();
	}

	@Reference
	void setLog(LogService log) {
		this.log = log;
	}

	@Reference
	public void setCoordinator(Coordinator coordinator) {
		this.coordinator = coordinator;
	}

	@Reference
	void setCache(Cache cache) {
		this.cache = cache;
	}
}