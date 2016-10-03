package osgi.enroute.updater.provider;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A very simple bundle that tracks bundles that have a location
 * that maps to a file. If so, it checks the lastModified times
 * of the bundle and if necessary refreshes the bundles.
 */
public class UpdaterImpl extends Thread implements BundleActivator {
	private static final String	REFERENCE		= "reference:";
	private static final int	SYSTEM_BUNDLE	= 0;
	private static Logger		logger			= LoggerFactory
			.getLogger(UpdaterImpl.class);

	static class BInfo {
		File	file;
		Bundle	bundle;
		long	lastPolled;
	}

	private BundleTracker<BInfo>	tracker;
	private FrameworkWiring			framework;
	private AtomicBoolean			inRefresh	= new AtomicBoolean(false);

	public UpdaterImpl() {
		super("OSGi Bundle Updater");
	}

	@Override
	public void start(BundleContext context) throws Exception {
		framework = context.getBundle(0).adapt(FrameworkWiring.class);
		tracker = new BundleTracker<BInfo>(context, -1, null) {
			@Override
			public BInfo addingBundle(Bundle bundle, BundleEvent event) {
				try {

					if (bundle.getBundleId() == SYSTEM_BUNDLE)
						return null;

					String location = bundle.getLocation();
					if (location.startsWith(REFERENCE))
						location = location.substring(REFERENCE.length());

					if (!location.startsWith("file:"))
						return null;

					File f = Paths.get(new URI(location)).toFile();
					if (!f.exists())
						return null;

					BInfo b = new BInfo();
					b.bundle = bundle;
					return b;
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		};
		tracker.open();
		start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		interrupt();
	}

	public void run() {
		logger.info("Starting");
		try {
			while (!isInterrupted())
				try {
					logger.info("Sleep");
					sleep(1000);
					if (inRefresh.get())
						continue;

					Set<Bundle> bundles = new HashSet<Bundle>();

					long recently = System.currentTimeMillis() - 1000;
					for (BInfo b : tracker.getTracked().values()) {
						long lastModified = b.file.lastModified();

						if (lastModified > recently)
							continue;

						if (lastModified > b.bundle.getLastModified()) {
							bundles.add(b.bundle);
							b.bundle.stop();
						}
					}

					if (bundles.isEmpty())
						continue;

					logger.info("Update " + bundles);
					for (Bundle b : bundles) try {
						b.update();
						b.start();
					} catch(Exception e) {
						logger.error("Unexpected error updating bundle "+b, e);
					}

					logger.info("Refresh start");
					inRefresh.set(true);
					framework.refreshBundles(bundles, event -> {
						inRefresh.set(false);
						logger.info("Refresh end");
					});

				} catch (InterruptedException e) {
					return;
				} catch (Exception e) {
					logger.error("Unexpected error", e);
				}
		} finally {
			logger.info("Exiting");
		}
	}
}
