package osgi.enroute.web.server.cache;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.osgi.framework.*;
import org.osgi.service.component.annotations.*;

import aQute.lib.base64.Base64;
import aQute.lib.hex.*;
import osgi.enroute.web.server.provider.*;

@Component
public class CacheFactory {
	static final long		DEFAULT_NOT_FOUND_EXPIRATION	= TimeUnit.MINUTES.toMillis(20);

	// Make this configurable
	private long					expiration = DEFAULT_NOT_FOUND_EXPIRATION;

	@Activate
	void activate()
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
	}

	Cache newCache(File f, Bundle b, String path) throws Exception {
		return newCache(f, b, Etag.get(f), path);
	}

	Cache newCache(File f, Properties mimes) throws Exception {
		return newCache(f, null, f.getAbsolutePath());
	}

	Cache newCache(Future<File> future) {
		return new Cache(future);
	}

	Cache newCache(File f, Bundle b, byte[] etag, String path) {
		Cache cache = new Cache();
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
}
