package osgi.enroute.web.server.provider;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;
import osgi.enroute.web.server.cache.*;

/**
 * This class creates a cached file that contains contributions from a set of
 * plugins. A bundle can register a service with the
 * {@link #PLUGIN} property. This must be a String+ identifying the
 * applications this plugin is part of. It should then set
 * {@value #CONTRIBUTIONS} with the paths to Javascript resources
 * that should be included. This class will then concatenate all these JS
 * resources into file. If the underlying plugins change then the file will be
 * refreshed.
 * <p>
 * A file that wants to use this model should define the following tags (the .js extension will be stripped if present):
 * <pre>
 * 	<script src="/osgi.enroute.contributions/my.app.js">
 * </pre>
 * This will then automatically include all 
 */
public class PluginContributions extends HttpServlet implements Closeable {
	static final String PLUGIN = "osgi.enroute.plugin.for";
	public static final String CONTRIBUTIONS = "osgi.enroute.contributions";

	private static Pattern EXTENSION = Pattern.compile("(.+)\\.js",Pattern.CASE_INSENSITIVE);
	private static final long serialVersionUID = 1L;
	private static final Set<String> EMPTY = new HashSet<String>();
	ServiceTracker<Object, ServiceReference<?>> pluginTracker;
	WebServer webserver;
	Cache cacheFactory;
	Map<String, PluginCache> pluginCache = new ConcurrentHashMap<>();

	public PluginContributions(WebServer w, Cache cacheFactory, BundleContext context)
			throws InvalidSyntaxException {
		this.webserver = w;
		this.cacheFactory = cacheFactory;
		pluginTracker = new ServiceTracker<Object, ServiceReference<?>>(context,
				FrameworkUtil.createFilter("(" + PLUGIN + "=*)"),
				null) {
			@Override
			public ServiceReference<?> addingService(
					ServiceReference<Object> ref) {
				try {
					Set<String> applications = toSet(ref
							.getProperty(PLUGIN));
					for (String app : applications) {
						PluginCache pc;
						synchronized (pluginCache) {
							pc = pluginCache.get(app);
							if (pc == null) {
								pc = cacheFactory.newPluginCache(webserver, pluginTracker, app);
								pluginCache.put(app, pc);
							}
						}
						synchronized (pc) {
							pc.dependencies.add(ref);
						}
					}
					return ref;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public void removedService(ServiceReference<Object> ref,
					ServiceReference<?> t) {
				Set<String> applications = toSet(ref
						.getProperty(PLUGIN));
				for (String app : applications) {
					PluginCache pc;
					synchronized (pluginCache) {
						pc = pluginCache.get(app);
						if (pc != null) {
							pc.dependencies.remove(ref);
							if (pc.dependencies.isEmpty())
								pluginCache.remove(pc);
						}
					}
				}
			}
		};
		pluginTracker.open(true);
	}
	
	public void close() {
		pluginTracker.close();
	}

	public FileCache findCachedPlugins(String path) {
		Matcher m = EXTENSION.matcher(path);
		if ( !m.matches())
			return pluginCache.get(path);;
		
		return pluginCache.get(m.group(1));
	}

	Set<String> toSet(Object object) {
		try {
			return Converter.cnv(new TypeReference<Set<String>>() {
			}, object);
		} catch (Exception e) {
			return EMPTY;
		}
	}

}
