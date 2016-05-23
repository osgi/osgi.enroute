package osgi.enroute.web.server.cache;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.zip.*;

import org.osgi.framework.*;
import org.osgi.service.component.annotations.*;
import org.osgi.service.log.*;
import org.osgi.util.tracker.*;

import aQute.lib.base64.Base64;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.libg.cryptography.*;
import osgi.enroute.web.server.provider.*;

@Component( service = Cache.class )
public class Cache {
	static final long		DEFAULT_NOT_FOUND_EXPIRATION	= TimeUnit.MINUTES.toMillis(20);

	// Make this configurable
	private long					expiration = DEFAULT_NOT_FOUND_EXPIRATION;

	File							cacheFile;
	private Executor				executor;
	LogService						log;

	@Activate
	void activate(BundleContext context)
		throws Exception
	{
		InputStream in = WebServer.class.getResourceAsStream("mimetypes");
		if (in != null)
			try {
				Mimes.mimes.load(in);
			}
			finally {
				in.close();
			}

		cacheFile = context.getDataFile("cache");
		cacheFile.mkdir();
	}

	public FileCache newFileCache(File f, Bundle b, String path) throws Exception {
		return newFileCache(f, b, Etag.get(f), path);
	}

	public FileCache newFileCache(File f) throws Exception {
		return newFileCache(f, null, f.getAbsolutePath());
	}

	public FileCache newFileCache(Future<File> future) {
		return new FileCache(future);
	}

	public FileCache newFileCache(File f, Bundle b, byte[] etag, String path) {
		FileCache cache = new FileCache();
		cache.time = f.lastModified();
		cache.bundle = b;
		cache.file = f;
		cache.etag = Hex.toHexString(etag);
		cache.md5 = Base64.encodeBase64(etag);
		if (b != null && b.getLastModified() > f.lastModified()) {
			cache.time = b.getLastModified();
			cache.file.setLastModified(cache.time);
		}
		int n = path.lastIndexOf('.');
		if (n > 0) {
			String ext = path.substring(n + 1);
			cache.mime = Mimes.mimes().getProperty(ext);
		}
		cache.expiration = expiration;

		return cache;
	}

	public PluginCache newPluginCache(WebServer webServer, ServiceTracker<Object, ServiceReference<?>> pluginTracker, String application) throws Exception {
		FileCache cache = newFileCache(File.createTempFile(application, ".js"));
		return new PluginCache(cache, webServer, pluginTracker);
	}
//
//	public File cacheFile()
//	{
//		return cacheFile;
//	}
//
	public File getCachedRawFile(String path) throws Exception {
		String name = SHA1.digest(path.getBytes("UTF-8")).asHex();
		return new File(cacheFile, name);
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
	public FileCache findCachedUrl(final String path) throws Exception {
		final File cached = getCachedRawFile(path);
		if (cached.isFile())
			return newFileCache(cached);

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
					File tmp = IO.createTempFile(cacheFile, "path", ".tmp");

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
		return newFileCache(task);
	}

	@Reference
	void setExecutor(Executor exe) {
		this.executor = exe;
	}

	@Reference
	void setLog(LogService log) {
		this.log = log;
	}
}
