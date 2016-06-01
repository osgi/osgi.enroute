package osgi.enroute.web.server.cache;

import java.io.*;
import java.util.concurrent.*;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import aQute.lib.base64.*;
import aQute.lib.hex.*;
import osgi.enroute.web.server.exceptions.*;
import osgi.enroute.web.server.provider.*;

public class CacheFileFactory {

	public static CacheFile newCacheFile(File f, Bundle b, long expiration, String path) throws NotFound404Exception, InternalServer500Exception {
		return newCacheFile(f, b, Etag.get(f), expiration, path);
	}

	public static CacheFile newCacheFile(File f, long expiration) throws NotFound404Exception, InternalServer500Exception {
		return newCacheFile(f, null, expiration, f.getAbsolutePath());
	}

	public static CacheFile newCacheFile(Future<File> future) {
		return new CacheFile(future);
	}

	public static CacheFile newCacheFile(File f, Bundle b, byte[] etag, long expiration, String path) {
		CacheFile c = new CacheFile();
		c.time = f.lastModified();
		c.bundle = b;
		c.file = f;
		c.etag = Hex.toHexString(etag);
		c.md5 = Base64.encodeBase64(etag);
		if (b != null && b.getLastModified() > f.lastModified()) {
			c.time = b.getLastModified();
			c.file.setLastModified(c.time);
		}
		int n = path.lastIndexOf('.');
		if (n > 0) {
			String ext = path.substring(n + 1);
			c.mime = Mimes.mimes().getProperty(ext);
		}
		c.expiration = expiration;

		return c;
	}

	public static PluginCacheFile newPluginCacheFile(WebServer2 webServer, ServiceTracker<Object, ServiceReference<?>> pluginTracker, String application, long expiration) throws WebServerException {
		try {
			CacheFile cache = newCacheFile(File.createTempFile(application, ".js"), expiration);
			return new PluginCacheFile(cache, webServer, pluginTracker);
		}
		catch (IOException e) {
			throw new InternalServer500Exception();
		}
	}
}
