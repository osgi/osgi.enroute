package osgi.enroute.web.server.provider;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
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

import osgi.enroute.web.server.provider.WebServer.Cache;
import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;
import aQute.lib.io.IO;

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
	static final String CONTRIBUTIONS = "osgi.enroute.contributions";
	
	private static Pattern EXTENSION = Pattern.compile("(.+)\\.js",Pattern.CASE_INSENSITIVE);
	private static final long serialVersionUID = 1L;
	private static final Set<String> EMPTY = new HashSet<String>();
	ServiceTracker<Object, ServiceReference<?>> pluginTracker;
	WebServer webserver;
	Map<String, PluginCache> pluginCache = new ConcurrentHashMap<>();

	class PluginCache extends Cache {

		private int count = -1;
		Set<ServiceReference<?>> dependencies = new HashSet<>();

		public PluginCache(WebServer webServer, String application)
				throws Exception {
			webServer.super(File.createTempFile(application, ".js"));
		}

		public boolean sync() {
			while (count != pluginTracker.getTrackingCount()) {
				int tmp = pluginTracker.getTrackingCount();
				try {
					build();
				} catch (Exception e) {
					return false;
				}
				count = tmp;
			}
			return true;
		}

		void build() throws Exception {
			synchronized (this) {
				try (FileOutputStream fout = new FileOutputStream(file)) {
					PrintStream p = new PrintStream(fout);

					for (ServiceReference<?> ref : dependencies) {
						Set<String> contributions = toSet(ref
								.getProperty(CONTRIBUTIONS));
						for (String contribution : contributions) {
							if (!contribution.startsWith("/"
									+ CONTRIBUTIONS)) {
								File f = webserver.getFile(contribution);

								if (f != null && f.isFile()) {
									IO.copy(f, fout);
								} else {
									p.printf("// not found %s\n", contribution);
								}
							}
						}
					}
				}
			}

		}
	}

	public PluginContributions(WebServer w, BundleContext context)
			throws InvalidSyntaxException {
		this.webserver = w;
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
								pc = new PluginCache(webserver, app);
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

	public Cache findCachedPlugins(String path) {
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
