package osgi.enroute.web.server.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipInputStream;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;

import osgi.enroute.capabilities.ServletWhiteboard;
import osgi.enroute.capabilities.WebServerExtender;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.lib.base64.Base64;
import aQute.lib.converter.Converter;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.libg.cryptography.Digester;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;

@WebServerExtender.Provide
@ServletWhiteboard.Require
@Component(provide = { Servlet.class }, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = {
		"alias=/", "name=" + WebServer.NAME }, name = WebServer.NAME)
public class WebServer extends HttpServlet {

	static final String NAME = "osgi.enroute.simple.server";

	public class RedirectException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private String path;

		public RedirectException(String path) {
			this.path = path;
		}

		public String getPath() {
			return path;
		}
	}

	private static final long DEFAULT_NOT_FOUND_EXPIRATION = TimeUnit.MINUTES
			.toMillis(20);
	static String BYTE_RANGE_SET_S = "(\\d+)?\\s*-\\s*(\\d+)?";
	static Pattern BYTE_RANGE_SET = Pattern.compile(BYTE_RANGE_SET_S);
	static Pattern BYTE_RANGE = Pattern
			.compile("bytes\\s*=\\s*(\\d+)?\\s*-\\s*(\\d+)?(?:\\s*,\\s*(\\d+)\\s*-\\s*(\\d+)?)*\\s*");
	private static final long serialVersionUID = 1L;
	static SimpleDateFormat format = new SimpleDateFormat(
			"EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
	Map<String, Cache> cached = new HashMap<String, Cache>();
	File cache;
	LogService log;
	Properties mimes = new Properties();
	boolean proxy;
	PluginContributions pluginContributions;

	static class Range {
		Range next;
		long start;
		long end;

		public long length() {
			if (next == null)
				return end - start;

			return next.length() + end - start;
		}

		Range(String range, long length) {
			if (range != null) {
				if (!BYTE_RANGE.matcher(range).matches())
					throw new IllegalArgumentException(
							"Bytes ranges does not match specification "
									+ range);

				Matcher m = BYTE_RANGE_SET.matcher(range);
				m.find();
				init(m, length);
			} else {
				start = 0;
				end = length;
			}
		}

		private Range() {
		}

		void init(Matcher m, long length) {
			String s = m.group(1);
			String e = m.group(2);
			if (s == null && e == null)
				throw new IllegalArgumentException(
						"Invalid range, both begin and end not specified: "
								+ m.group(0));

			if (s == null) { // -n == l-n -> l
				start = length - Long.parseLong(e);
				end = length - 1;
			} else if (e == null) { // n- == n -> l
				start = Long.parseLong(s);
				end = length - 1;
			} else {
				start = Long.parseLong(s);
				end = Long.parseLong(e);
			}
			end++; // e is specified as inclusive, Java uses exclusive

			if (end > length)
				end = length;

			if (start < 0)
				start = 0;

			if (start >= end)
				throw new IllegalArgumentException(
						"Invalid range, start higher than end " + m.group(0));

			if (m.find()) {
				next = new Range();
				next.init(m, length);
			}
		}

		void copy(FileChannel from, WritableByteChannel to) throws IOException {
			from.transferTo(start, end - start, to);
			if (next != null)
				next.copy(from, to);
		}
	}

	class Cache {
		long time;
		String etag;
		String md5;
		File file;
		Bundle bundle;
		String mime;
		long expiration;
		boolean publc;
		private Future<File> future;
		public boolean is404;

		Cache(File f, Bundle b, String path) throws Exception {
			this(f, b, getEtag(f), path);
		}

		Cache(File f, Bundle b, byte[] etag, String path) {
			this.time = f.lastModified();
			this.bundle = b;
			this.file = f;
			this.etag = Hex.toHexString(etag);
			this.md5 = Base64.encodeBase64(etag);
			if (b != null && b.getLastModified() > f.lastModified()) {
				this.time = b.getLastModified();
				this.file.setLastModified(this.time);
			}
			int n = path.lastIndexOf('.');
			if (n > 0) {
				String ext = path.substring(n + 1);
				this.mime = mimes.getProperty(ext);
			}
		}

		public Cache(File f) throws Exception {
			this(f, null, f.getAbsolutePath());
		}

		public Cache(Future<File> future) {
			this.future = future;
		}

		// Should be called on
		// caches so that we can do any work outside the
		// locks
		public boolean sync() throws Exception {
			if (this.future == null)
				return file != null;

			try {
				this.file = this.future.get();
				byte[] etag = getEtag(this.file);
				this.etag = Hex.toHexString(etag);
				this.md5 = Base64.encodeBase64(etag);
				int n = file.getAbsolutePath().lastIndexOf('.');
				if (n > 0) {
					String ext = file.getAbsolutePath().substring(n + 1);
					this.mime = mimes.getProperty(ext);
				}
				return true;
			} catch (Exception e) {
				expiration = System.currentTimeMillis()
						+ DEFAULT_NOT_FOUND_EXPIRATION;
				return false;
			}
		}

		boolean isExpired() {
			if (expiration >= System.currentTimeMillis())
				return true;

			if (file == null && future != null)
				return false;

			if (!file.isFile())
				return true;

			if (time < file.lastModified())
				return true;

			if (bundle != null && bundle.getLastModified() > time)
				return true;

			if (expiration >= System.currentTimeMillis())
				return true;

			return false;
		}

		public boolean isNotFound() {
			// TODO Auto-generated method stub
			return false;
		}
	}

	static byte[] getEtag(File f) throws Exception {
		if (!f.isFile())
			throw new IllegalArgumentException("not a file (anymore?) " + f);
		Digester<MD5> digester = MD5.getDigester();
		IO.copy(f, digester);
		return digester.digest().digest();
	}

	interface Config {
		String alias();

		boolean noBundles();

		File[] directories();

		int expires();

		boolean exceptions();

		boolean debug();

		boolean noproxy();

		List<String> mimes();

		long expiration();

		int maxConnections();

		String maxConnectionMessage();

		int maxTime();

		String maxTimeMessage();

		String redirect();
	}

	Config config;
	BundleTracker<?> tracker;
	private Executor executor;
	private ServiceRegistration<Filter> webfilter;
	private String alias;
	private String redirect = "/index.html";
	private Coordinator coordinator;
	private ServiceRegistration<Filter> exceptionFilter;

	@Activate
	void activate(Map<String, Object> props, BundleContext context)
			throws Exception {
		this.config = Converter.cnv(Config.class, props);
		proxy = !config.noproxy();
		if (config.redirect() != null)
			redirect = config.redirect();

		alias = config.alias();
		if (alias == null || alias.isEmpty())
			alias = "/";

		pluginContributions = new PluginContributions(this,context);
		
		InputStream in = WebServer.class.getResourceAsStream("mimetypes");
		if (in != null)
			try {
				mimes.load(in);
			} finally {
				in.close();
			}

		if (config.mimes() != null)
			for (String mime : config.mimes()) {
				String parts[] = mime.trim().split("\\s*=\\s*");
				if (parts.length == 2) {
					if (parts[0].startsWith("."))
						parts[0] = parts[0].substring(1);
					mimes.put(parts[0], parts[1]);
				}
			}

		this.cache = context.getDataFile("cache");
		cache.mkdir();

		tracker = new BundleTracker<Bundle>(context, Bundle.ACTIVE
				| Bundle.STARTING, null) {
			public Bundle addingBundle(Bundle bundle, BundleEvent event) {
				if (bundle.getEntryPaths("static/") != null)
					return bundle;
				return null;
			}
		};
		tracker.open();

		Hashtable<String, Object> p = new Hashtable<String, Object>();
		p.put("pattern", ".*");
		webfilter = context.registerService(
				Filter.class,
				new WebFilter(config.maxConnections(), config
						.maxConnectionMessage(), coordinator), p);

		if (config.exceptions()) {
			p.putAll(props);
			exceptionFilter = context.registerService(Filter.class,
					new ExceptionFilter(), p);
		}
	}

	public boolean handleSecurity(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		return true;
	}

	public void doGet(HttpServletRequest rq, HttpServletResponse rsp)
			throws IOException, ServletException {
		try {
			String path = rq.getPathInfo();
			if (path == null || path.isEmpty() || path.equals("/")) {
				throw new RedirectException(redirect);
			} else if (path.startsWith("/"))
				path = path.substring(1);

			if (path.endsWith("/")) {
				throw new RedirectException("/" + path + "index.html");
			}


			Cache c = getCache(path);

			if (c == null || !c.sync()) {
				rsp.sendError(HttpServletResponse.SC_NOT_FOUND, "File " + path
						+ " could not be found");
				return;
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
				throw new IllegalArgumentException(
						"Range to read is too high: " + length);

			rsp.setContentLength((int) range.length());

			if (config.expires() != 0) {
				Date expires = new Date(System.currentTimeMillis() + 60000
						* config.expires());
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
						return;
					}
				} catch (Exception e) {
					// e.printStackTrace();
				}
			}

			String ifNoneMatch = rq.getHeader("If-None-Match");
			if (ifNoneMatch != null) {
				if (ifNoneMatch.indexOf(c.etag) >= 0) {
					rsp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					return;
				}
			}

			if (c.is404)
				rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			else
				rsp.setStatus(HttpServletResponse.SC_OK);

			if (rq.getMethod().equalsIgnoreCase("GET")) {
				OutputStream out = rsp.getOutputStream();
				//
				// TODO make sure we do not
				// compress binaries.
				//
				String acceptEncoding = rq.getHeader("Accept-Encoding");
				// weird, when the file is < 30 bytes, the deflate
				// seems to loose 2 bytes :-( With a threshold
				// it seems to work.
				if (acceptEncoding != null && length > 100) {
					acceptEncoding = acceptEncoding.toUpperCase();
					boolean deflate = acceptEncoding.indexOf("deflate") >= 0;
					boolean gzip = acceptEncoding.toLowerCase().indexOf("gzip") >= 0;

					if (gzip) {
						out = new GZIPOutputStream(out);
						rsp.setHeader("Content-Encoding", "gzip");
					} else if (deflate) {
						out = new DeflaterOutputStream(out);
						rsp.setHeader("Content-Encoding", "deflate");
					}
				}
				FileInputStream file = new FileInputStream(c.file);
				try {
					FileChannel from = file.getChannel();
					WritableByteChannel to = Channels.newChannel(out);
					range.copy(from, to);
					from.close();
					to.close();
				} finally {
					file.close();
				}
				out.flush();
				rsp.getOutputStream().flush();
				rsp.getOutputStream().close();
				out.close();
			}
		} catch (RedirectException e) {
			rsp.sendRedirect(e.getPath());
		} catch (Exception e) {
			log.log(LogService.LOG_ERROR, "Internal webserver error", e);
			if (config.exceptions())
				throw new RuntimeException(e);

			try {
				PrintWriter pw = rsp.getWriter();
				pw.println("Internal server error\n");
				rsp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} catch (Exception ee) {
				log.log(LogService.LOG_ERROR,
						"Second level internal webserver error", ee);
			}
		}
	}

	Cache getCache(String path) throws Exception {
		Cache c;
		synchronized (cached) {
			c = cached.get(path);
			if (c == null || c.isExpired()) {
				c = find(path);
				if (c == null) {
					c = do404(path);
				} else
					cached.put(path, c);
			}
		}
		return c;
	}
	
	File getFile(String path) throws Exception {
		Cache c = getCache(path);
		if ( c == null)
			return null;

		if ( !c.sync() )
			return null;
		
		return c.file;
	}

	private Cache do404(String path) throws Exception {
		log.log(LogService.LOG_INFO, "404 " + path);
		Cache c = find("404.html");
		if (c == null)
			c = findBundle("default/404.html");
		if (c != null)
			c.is404=true;
		
		return c;
	}

	public void doHead(HttpServletRequest rq, HttpServletResponse rsp)
			throws IOException, ServletException {
		doGet(rq, rsp);
	}

	Cache find(String path) throws Exception {
		if (proxy && path.startsWith("$"))
			return findCachedUrl(path);

		if (path.startsWith(PluginContributions.CONTRIBUTIONS+"/"))
			return pluginContributions.findCachedPlugins(path.substring(PluginContributions.CONTRIBUTIONS.length()+1));

		Cache c = findFile(path);
		if (c != null)
			return c;
		// if (config.noBundles())
		// return null;
		return findBundle(path);
	}

	/**
	 * HTTPS pages require that all content is actually HTTPS ... this means
	 * that any content not from our site ruins our green bar :-( So the
	 * webserver has a possibility to proxy other urls. For efficiency, it
	 * reuses the caching mechanism. It proxies any path that starts with $, it
	 * assumes the remainder is an encoded URL.
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	private Cache findCachedUrl(final String path) throws Exception {
		final File cached = getCached(path);
		if (cached.isFile())
			return new Cache(cached);

		cached.getAbsoluteFile().getParentFile().mkdirs();

		FutureTask<File> task = new FutureTask<File>(new Callable<File>() {

			@Override
			public File call() {
				try {
					String uri = URLDecoder.decode(path.substring(1), "UTF-8");
					URL url = new URL(uri);
					URLConnection con = url.openConnection();
					con.setConnectTimeout(10000);
					con.setRequestProperty("Accept-Encoding", "deflate, gzip");
					File tmp = IO.createTempFile(cache, "path", ".tmp");

					InputStream in = con.getInputStream();
					String encoding = con.getContentEncoding();
					if ("deflate".equalsIgnoreCase(encoding)) {
						in = new DeflaterInputStream(in);
					} else if ("gzip".equalsIgnoreCase(encoding)) {
						in = new ZipInputStream(in);
					}

					IO.copy(in, tmp);
					IO.rename(tmp, cached);
					cached.setLastModified(con.getLastModified() + 1000);
					return cached;
				} catch (Exception e) {
					log.log(LogService.LOG_ERROR, "Cannot read url " + path);
					throw new RuntimeException(e);
				}
			}

		});
		executor.execute(task);
		return new Cache(task);
	}

	Cache findFile(String path) throws Exception {
		if (config.directories() != null)
			for (File base : config.directories()) {
				File f = IO.getFile(base, path);

				if (f.isDirectory())
					f = new File(f, "index.html");

				if (f.isFile()) {
					return new Cache(f);
				}
			}
		return null;
	}

	Cache findBundle(String path) throws Exception {
		Bundle[] bundles = tracker.getBundles();
		if (bundles != null) {
			for (Bundle b : bundles) {
				Enumeration<URL> urls = b.findEntries("static/" + path, "*",
						false);
				if (urls != null && urls.hasMoreElements()) {
					throw new RedirectException("/" + path + "/index.html");
				}
				URL url = null;
				if (config.debug()) {
					url = b.getResource("static/debug/" + path);
				}
				if (url == null) {
					url = b.getResource("static/" + path);
				}
				if (url == null)
					url = b.getResource("static/" + path + "/index.html");
				if (url != null) {
					File cached = getCached(path);
					if (!cached.exists()
							|| cached.lastModified() <= b.getLastModified()) {
						cached.delete();
						cached.getAbsoluteFile().getParentFile().mkdirs();
						FileOutputStream out = new FileOutputStream(cached);
						Digester<MD5> digester = MD5.getDigester(out);
						IO.copy(url.openStream(), digester);
						digester.close();
						cached.setLastModified(b.getLastModified() + 1000);
						return new Cache(cached, b, digester.digest().digest(),
								path);
					}
					return new Cache(cached, b, path);
				}
			}
		}
		return null;
	}

	private File getCached(String path) throws Exception {
		String name = SHA1.digest(path.getBytes("UTF-8")).asHex();
		return new File(cache, name);
	}

	public String getMimeType(String name) {
		// TODO
		return null;
	}

	@Deactivate
	void deactivate() {
		tracker.close();
		pluginContributions.close();
		if (exceptionFilter != null)
			webfilter.unregister();
		if (exceptionFilter != null)
			exceptionFilter.unregister();
	}

	@Reference
	void setLog(LogService log) {
		this.log = log;
	}

	@Reference
	void setExecutor(Executor exe) {
		this.executor = exe;
	}

	@Reference
	public void setCoordinator(Coordinator coordinator) {
		this.coordinator = coordinator;
	}
}