package osgi.enroute.web.server.provider;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.stream.*;
import java.util.zip.*;

import javax.servlet.Filter;
import javax.servlet.http.*;

import org.osgi.framework.*;
import org.osgi.namespace.extender.*;
import org.osgi.service.component.annotations.*;
import org.osgi.service.coordinator.*;
import org.osgi.service.log.*;
import org.osgi.util.tracker.*;

import aQute.bnd.annotation.headers.*;
import aQute.lib.base64.Base64;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.json.*;
import aQute.libg.cryptography.*;
import aQute.libg.sed.*;
import osgi.enroute.dto.api.*;
import osgi.enroute.http.capabilities.*;
import osgi.enroute.servlet.api.*;
import osgi.enroute.web.server.provider.IndexDTO.*;
import osgi.enroute.webserver.capabilities.*;

@ProvideCapability(ns = ExtenderNamespace.EXTENDER_NAMESPACE, name = WebServerConstants.WEB_SERVER_EXTENDER_NAME, version = WebServerConstants.WEB_SERVER_EXTENDER_VERSION)
@RequireHttpImplementation
@Component(service = {
		ConditionalServlet.class
}, immediate = true, property = {
		"service.ranking:Integer=1000", "name=" + WebServer.NAME, "no.index=true"
}, name = WebServer.NAME, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class WebServer implements ConditionalServlet {

	static final String NAME = "osgi.enroute.simple.server";

	static final long		DEFAULT_NOT_FOUND_EXPIRATION	= TimeUnit.MINUTES.toMillis(20);
	static String			BYTE_RANGE_SET_S				= "(\\d+)?\\s*-\\s*(\\d+)?";
	static Pattern			BYTE_RANGE_SET					= Pattern.compile(BYTE_RANGE_SET_S);
	static Pattern			BYTE_RANGE						= Pattern
			.compile("bytes\\s*=\\s*(\\d+)?\\s*-\\s*(\\d+)?(?:\\s*,\\s*(\\d+)\\s*-\\s*(\\d+)?)*\\s*");
	static SimpleDateFormat	format							= new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",
			Locale.ENGLISH);
	Map<String,Cache>		cached							= new HashMap<String,Cache>();
	File					cache;
	LogService				log;
	Properties				mimes							= new Properties();
	boolean					proxy;
	PluginContributions		pluginContributions;
	WebResources			webResources;
	IndexDTO				index							= new IndexDTO();
	DTOs					dtos;

	static class Range {
		Range	next;
		long	start;
		long	end;

		public long length() {
			if (next == null)
				return end - start;

			return next.length() + end - start;
		}

		Range(String range, long length) {
			if (range != null) {
				if (!BYTE_RANGE.matcher(range).matches())
					throw new IllegalArgumentException("Bytes ranges does not match specification " + range);

				Matcher m = BYTE_RANGE_SET.matcher(range);
				m.find();
				init(m, length);
			} else {
				start = 0;
				end = length;
			}
		}

		private Range() {}

		void init(Matcher m, long length) {
			String s = m.group(1);
			String e = m.group(2);
			if (s == null && e == null)
				throw new IllegalArgumentException("Invalid range, both begin and end not specified: " + m.group(0));

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
				throw new IllegalArgumentException("Invalid range, start higher than end " + m.group(0));

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
		long					time;
		String					etag;
		String					md5;
		File					file;
		Bundle					bundle;
		String					mime;
		long					expiration;
		boolean					publc;
		private Future<File>	future;
		public boolean			is404;

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
			}
			catch (Exception e) {
				expiration = System.currentTimeMillis() + DEFAULT_NOT_FOUND_EXPIRATION;
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

	@interface Config {
		String osgi_http_whiteboard_servlet_pattern();

		boolean noBundles();

		String[]directories() default {};

		int expires();

		boolean exceptions();

		boolean debug();

		boolean noproxy();

		String[]mimes();

		long expiration();

		int maxConnections();

		String maxConnectionMessage();

		int maxTime();

		String maxTimeMessage();
	}

	Config								config;
	BundleTracker< ? >					tracker;
	private Executor					executor;
	private ServiceRegistration<Filter>	webfilter;
	private Coordinator					coordinator;
	private ServiceRegistration<Filter>	exceptionFilter;
	private BundleTracker<Bundle>		apps;
	private List<File>					directories	= Collections.emptyList();

	@Activate
	void activate(Config config, Map<String,Object> props, BundleContext context) throws Exception {
		index.configuration = props;
		this.config = config;
		proxy = !config.noproxy();

		String[] directories = config.directories();
		if (directories != null)
			this.directories = Stream.of(directories).map((b) -> IO.getFile(b)).collect(Collectors.toList());

		pluginContributions = new PluginContributions(this, context);
		webResources = new WebResources(this, context);

		InputStream in = WebServer.class.getResourceAsStream("mimetypes");
		if (in != null)
			try {
				mimes.load(in);
			}
			finally {
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

		tracker = new BundleTracker<Bundle>(context, Bundle.ACTIVE | Bundle.STARTING, null) {
			public Bundle addingBundle(Bundle bundle, BundleEvent event) {
				if (bundle.getEntryPaths("static/") != null)
					return bundle;
				return null;
			}
		};
		tracker.open();

		Hashtable<String,Object> p = new Hashtable<String,Object>();
		p.put("pattern", ".*");
		webfilter = context.registerService(Filter.class,
				new WebFilter(config.maxConnections(), config.maxConnectionMessage(), coordinator), p);

		if (config.exceptions()) {
			p.putAll(props);
			exceptionFilter = context.registerService(Filter.class, new ExceptionFilter(), p);
		}

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

	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		return true;
	}

	@Override
	public boolean doConditionalService(HttpServletRequest rq, HttpServletResponse rsp) throws Exception {
		try {
			String path = rq.getRequestURI();
			if (path != null && path.startsWith("/"))
				path = path.substring(1);

			Cache c = getCache(path);

			if (c == null || !c.sync()) {
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

	private void index(HttpServletResponse rsp) throws Exception {
		Cache c = getCache("osgi/enroute/web/index.html");
		if (c == null || c.is404 || c.isNotFound()) {
			c = getCache("osgi/enroute/web/local/index.html");
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
		if (c == null)
			return null;

		if (!c.sync())
			return null;

		return c.file;
	}

	private Cache do404(String path) throws Exception {
		log.log(LogService.LOG_INFO, "404 " + path);
		Cache c = find("404.html");
		if (c == null)
			c = findBundle("default/404.html");
		if (c != null)
			c.is404 = true;

		return c;
	}

	Cache find(String path) throws Exception {
		if (proxy && path.startsWith("$"))
			return findCachedUrl(path);

		if (path.startsWith(PluginContributions.CONTRIBUTIONS + "/"))
			return pluginContributions
					.findCachedPlugins(path.substring(PluginContributions.CONTRIBUTIONS.length() + 1));

		Cache c = webResources.find(path);
		if (c != null)
			return c;

		c = findFile(path);
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
				}
				catch (Exception e) {
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
			for (File base : directories) {
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
				Enumeration<URL> urls = b.findEntries("static/" + path, "*", false);
				// What happens here is that we have hit a folder, but the path does not
				// end with a "/". I do not think that it is correct to do a redirect here.
				// In any case, when redirects are turned off, this causes an infinite redirect loop.
				// Instead, a 404 should be thrown.
				// I would argue that a 404 should **always** be thrown here for this case.
				if (urls != null && urls.hasMoreElements()) {
//					throw new RedirectException("/" + path);
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
					if (!cached.exists() || cached.lastModified() <= b.getLastModified()) {
						cached.delete();
						cached.getAbsoluteFile().getParentFile().mkdirs();
						FileOutputStream out = new FileOutputStream(cached);
						Digester<MD5> digester = MD5.getDigester(out);
						IO.copy(url.openStream(), digester);
						digester.close();
						cached.setLastModified(b.getLastModified() + 1000);
						return new Cache(cached, b, digester.digest().digest(), path);
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

		apps.close();
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

	@Reference
	void setDTOs(DTOs dtos) {
		this.dtos = dtos;
	}
}