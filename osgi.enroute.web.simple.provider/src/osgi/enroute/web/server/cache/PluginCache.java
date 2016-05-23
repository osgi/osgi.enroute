package osgi.enroute.web.server.cache;

import java.io.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import aQute.lib.converter.*;
import aQute.lib.io.*;
import osgi.enroute.web.server.provider.*;

public class PluginCache extends FileCache {
	private static final Set<String> EMPTY = new HashSet<String>();

	private int count = -1;
	private WebServer webServer;
	private ServiceTracker<Object, ServiceReference<?>> pluginTracker;
	public Set<ServiceReference<?>> dependencies = new HashSet<>();

	PluginCache(FileCache cache, WebServer webServer, ServiceTracker<Object, ServiceReference<?>> pluginTracker) {
		this(cache.time, cache.etag, cache.md5, cache.file, cache.bundle, cache.mime, cache.expiration, cache.publc, cache.is404, webServer, pluginTracker);
	}

	PluginCache(
			long			time,
			String			etag,
			String			md5,
			File			file,
			Bundle			bundle,
			String			mime,
			long			expiration,
			boolean			publc,
			boolean			is404,
			WebServer 		webServer,
			ServiceTracker<Object, ServiceReference<?>> pluginTracker) {
		this.time = time;
		this.etag = etag;
		this.md5 = md5;
		this.file = file;
		this.bundle = bundle;
		this.mime = mime;
		this.expiration = expiration;
		this.publc = publc;
		this.is404 = is404;
		this.pluginTracker = pluginTracker;
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
							.getProperty(PluginContributions.CONTRIBUTIONS));
					for (String contribution : contributions) {
						if (!contribution.startsWith("/"
								+ PluginContributions.CONTRIBUTIONS)) {
							File f = webServer.getFile(contribution);

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

	Set<String> toSet(Object object) {
		try {
			return Converter.cnv(new TypeReference<Set<String>>() {
			}, object);
		} catch (Exception e) {
			return EMPTY;
		}
	}
}
