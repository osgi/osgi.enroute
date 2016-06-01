package osgi.enroute.web.server.cache;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.zip.*;

import org.osgi.framework.*;
import org.osgi.service.component.annotations.*;
import org.osgi.service.log.*;

import aQute.lib.io.*;
import aQute.libg.cryptography.*;
import osgi.enroute.web.server.cache.CacheFileFactory.*;
import osgi.enroute.web.server.config.*;
import osgi.enroute.web.server.exceptions.*;
import osgi.enroute.web.server.provider.*;

@Component( 
		service = Cache.class,
		name = Cache.NAME, 
		configurationPid = BundleMixinServer.NAME,
		configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class Cache {
	static final String NAME = "osgi.enroute.simple.server.cache";

	static final long				DEFAULT_NOT_FOUND_EXPIRATION = TimeUnit.MINUTES.toMillis(20);

	private long					expiration = DEFAULT_NOT_FOUND_EXPIRATION;

	File							cacheFile;
	private Executor				executor;
	LogService						log;
	WebServerConfig					config;

	private final Map<String,CacheFile>	cached = new HashMap<String,CacheFile>();
	private Lock 					lock = new ReentrantLock();

	@Activate
	void activate(WebServerConfig config, BundleContext context)
		throws Exception
	{
		this.config = config;
		InputStream in = Cache.class.getResourceAsStream("mimetypes");
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

	private File getRawFile(String path) throws InternalServer500Exception {
		try {
			String name = SHA1.digest(path.getBytes("UTF-8")).asHex();
			return new File(cacheFile, name);
		}
		catch (Exception e) {
			throw new InternalServer500Exception(e);
		}
	}

	/**
	 * HTTPS pages require that all content is actually HTTPS ... this means
	 * that any content not from our site ruins our green bar :-( So the
	 * webserver has a possibility to proxy other urls. For efficiency, it
	 * reuses the caching mechanism. It proxies any path that starts with $, it
	 * assumes the remainder is an encoded URL.
	 */
	public CacheFile findCacheFileByPath(final String path) throws NotFound404Exception, InternalServer500Exception {
		final File cached = getRawFile(path);
		if (cached.isFile())
			return CacheFileFactory.newCacheFile(cached, expiration);

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
					throw new RuntimeException(new InternalServer500Exception(e));
				}
			}

		});
		executor.execute(task);
		return CacheFileFactory.newCacheFile(task);
	}

	/**
	 * Returns an "internal" URL of the existing file in the bundle or null if there is no
	 * file corresponding to the path. This URL is only used for accessing the file content; it
	 * is NOT and must not be exposed externally.
	 */
	public URL internalUrlOf(Bundle b, String path) {
		Enumeration<URL> urls;
		if (config.debug())
			urls = b.findEntries("static/debug/" + path, "*", false);
		else
			urls = b.findEntries("static/" + path, "*", false);

		// We have hit a folder
		if (urls != null && urls.hasMoreElements()) {
			return null;
		}

		URL url = null;
		if (config.debug()) {
			url = b.getResource("static/debug/" + path);
		}
		else if (url == null) {
			url = b.getResource("static/" + path);
		}

		return url;
	}

	public CacheFile getFromBundle(Bundle b, URL url, String path) throws InternalServer500Exception {
		try {
			if (url == null )
				return null;

			File cached = getRawFile(path);
			if (!cached.exists() || cached.lastModified() <= b.getLastModified()) {
				cached.delete();
				cached.getAbsoluteFile().getParentFile().mkdirs();
				FileOutputStream out = new FileOutputStream(cached);
				Digester<MD5> digester = MD5.getDigester(out);
				IO.copy(url.openStream(), digester);
				digester.close();
				cached.setLastModified(b.getLastModified() + 1000);
				return CacheFileFactory.newCacheFile(cached, b, digester.digest().digest(), expiration, path);
			}

			return CacheFileFactory.newCacheFile(cached, b, expiration, path);
		}
		catch (Exception e) {
			throw new InternalServer500Exception(e);
		}
	}

	public CacheFile get(String path)
	{
		lock.lock();
		try {
			return cached.get(path);
		} finally {
			lock.unlock();
		}
	}

	public void put(String path, CacheFile c)
	{
		lock.lock();
		try {
			cached.put(path, c);
		} finally {
			lock.unlock();
		}
	}

	public void lock()
	{
		lock.lock();
	}

	public void unlock()
	{
		lock.unlock();
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
