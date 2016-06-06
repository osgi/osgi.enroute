package osgi.enroute.web.server.cache;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import aQute.lib.base64.Base64;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.libg.cryptography.*;
import osgi.enroute.web.server.exceptions.*;
import osgi.enroute.web.server.provider.*;

public class CacheFileFactory {

	public static class Etag {
		static byte[] get(File f) throws NotFound404Exception, InternalServer500Exception {
			try {
				if (!f.isFile())
					// Shouldn't this be a 500 instead??
					throw new NotFound404Exception(null, new IllegalArgumentException("not a file (anymore?) " + f));
				Digester<MD5> digester = MD5.getDigester();
				IO.copy(f, digester);
				return digester.digest().digest();
			}
			catch (NotFound404Exception e ) {
				throw e;
			}
			catch (Exception e) {
				throw new InternalServer500Exception(e);
			}
		}
	}

	public static class Mimes {
		static Properties				mimes							= new Properties();

		static Properties mimes() {
			Properties copy = new Properties(mimes);
			return copy;
		}
	}

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

	public static PluginCacheFile newPluginCacheFile(WebresourceServer webServer, ServiceTracker<Object, ServiceReference<?>> pluginTracker, String application, long expiration) throws WebServerException {
		try {
			CacheFile cache = newCacheFile(File.createTempFile(application, ".js"), expiration);
			return new PluginCacheFile(cache, webServer, pluginTracker);
		}
		catch (IOException e) {
			throw new InternalServer500Exception();
		}
	}
}
