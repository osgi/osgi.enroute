package osgi.enroute.web.server.cache;

import java.io.*;
import java.util.concurrent.*;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import osgi.enroute.web.server.provider.*;

public interface CacheFactory {
	static final long		DEFAULT_NOT_FOUND_EXPIRATION	= TimeUnit.MINUTES.toMillis(20);

	Cache newCache(File f, Bundle b, String path) throws Exception;
	Cache newCache(File f) throws Exception;
	Cache newCache(Future<File> future);
	Cache newCache(File f, Bundle b, byte[] etag, String path);
	PluginCache newPluginCache(
			WebServer webServer, 
			ServiceTracker<Object, ServiceReference<?>> pluginTracker, 
			String application) throws Exception;
}
