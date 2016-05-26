package osgi.enroute.web.server.provider;

import java.io.*;
import java.nio.channels.*;
import java.text.*;
import java.util.*;
import java.util.stream.*;

import javax.servlet.http.*;

import org.osgi.framework.*;
import org.osgi.namespace.extender.*;
import org.osgi.service.component.annotations.*;
import org.osgi.service.log.*;
import org.osgi.util.tracker.*;

import aQute.bnd.annotation.headers.*;
import aQute.lib.io.*;
import osgi.enroute.dto.api.*;
import osgi.enroute.http.capabilities.*;
import osgi.enroute.servlet.api.*;
import osgi.enroute.web.server.cache.*;
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
				"name=" + WebServer.NAME, 
				"no.index=true" }, 
		name = WebServer.NAME, 
		configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class WebServer implements ConditionalServlet {

	static final String NAME = "osgi.enroute.simple.server";

	static SimpleDateFormat	format							= new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",
			Locale.ENGLISH);
	LogService				log;
	DTOs					dtos;
	Cache					cache;

	WebServerConfig						config;
	BundleTracker< ? >					tracker;
	private List<File>					directories	= Collections.emptyList();

	@Activate
	void activate(WebServerConfig config, Map<String,Object> props, BundleContext context) throws Exception {
		this.config = config;

		String[] directories = config.directories();
		if (directories != null)
			this.directories = Stream.of(directories).map((b) -> IO.getFile(b)).collect(Collectors.toList());

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
		catch (RedirectException e) {
			rsp.sendRedirect(e.getPath());
		}
		catch (Exception e) {
			log.log(LogService.LOG_ERROR, "Internal webserver error", e);
			if (config.exceptions())
				throw new RuntimeException(e);

			try {
				PrintWriter pw = rsp.getWriter();
				pw.println("Internal server error\n");
				rsp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
			catch (Exception ee) {
				log.log(LogService.LOG_ERROR, "Second level internal webserver error", ee);
			}
		}
		return true;
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
		if (c == null)
			c = findBundle("default/404.html");
		if (c != null)
			c.is404 = true;

		return c;
	}

	FileCache find(String path) throws Exception {
		FileCache c = findFile(path);
		if (c != null)
			return c;
		return findBundle(path);
	}

	FileCache findFile(String path) throws Exception {
		if (config.directories() != null)
			for (File base : directories) {
				File f = IO.getFile(base, path);

				if (f.isDirectory())
					f = new File(f, "index.html");

				if (f.isFile()) {
					return cache.newFileCache(f);
				}
			}
		return null;
	}

	FileCache findBundle(String path) throws Exception {
		Bundle[] bundles = tracker.getBundles();
		if (bundles != null) {
			for (Bundle b : bundles) {
				FileCache c = cache.getFromBundle(b, path);
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
	void setDTOs(DTOs dtos) {
		this.dtos = dtos;
	}

	@Reference
	void setCache(Cache cache) {
		this.cache = cache;
	}
}