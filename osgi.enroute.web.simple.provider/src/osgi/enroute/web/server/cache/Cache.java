package osgi.enroute.web.server.cache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.DeflaterInputStream;
import java.util.zip.ZipInputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;

import aQute.lib.io.IO;
import aQute.libg.cryptography.Digester;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;
import osgi.enroute.web.server.cache.CacheFileFactory.Mimes;
import osgi.enroute.web.server.config.WebServerConfig;
import osgi.enroute.web.server.exceptions.FolderException;
import osgi.enroute.web.server.exceptions.InternalServer500Exception;
import osgi.enroute.web.server.exceptions.NotFound404Exception;
import osgi.enroute.web.server.provider.BundleMixinServer;

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
	public URL internalUrlOf(Bundle b, String path) throws FolderException {
		String internalPath;
		if (config.debug())
			internalPath = "static/debug/" + path;
		else
			internalPath = "static/" + path;

		Enumeration<URL> urls = b.findEntries(internalPath, "*", false);

		// We have hit a folder
		if (urls != null && urls.hasMoreElements()) {
			throw new FolderException(path);
//			return null;
		}

		return b.getResource(internalPath);
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
