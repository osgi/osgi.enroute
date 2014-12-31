package osgi.enroute.equinox.log.adapter;

import java.util.Collection;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Equinox is suddenly registering a Log Service but for some mysterious reason
 * they did not want to add the traditional buffer. This kills Gogo and
 * WebConsole. This embedded bundle will remove the Equinox log from sight and
 * forwards any messages on its reader to the available log services.
 */

public class HideEquinoxLog implements BundleActivator, FindHook,
		EventListenerHook {
	
	public boolean IMMEDIATE = true;

	private ServiceReference<?> equinoxLogReaderRef;
	private ServiceReference<?> equinoxLogRef;

	@Override
	public void start(BundleContext context) throws Exception {
		try {
			ServiceTracker<LogService, LogService> tracker;

			//
			// Get the references to the log and reader services
			// registered by Equinox
			//

			equinoxLogReaderRef = context
					.getServiceReference(LogReaderService.class);
			equinoxLogRef = context.getServiceReference(LogService.class);

			//
			// Only have to do this when we actually have a registered log
			// service
			//

			if (equinoxLogReaderRef == null)
				return;

			LogReaderService lrs = (LogReaderService) context
					.getService(equinoxLogReaderRef);

			//
			// Maybe they fixed this bug in the mean time
			//

			if (lrs.getLog().hasMoreElements())
				return;

			//
			// Register the hooks that filter the Equinox log
			//

			context.registerService(FindHook.class, this, null);
			context.registerService(EventListenerHook.class, this, null);

			tracker = new ServiceTracker<LogService, LogService>(context,
					LogService.class, null) {
				@Override
				public LogService addingService(
						ServiceReference<LogService> reference) {

					//
					// We must of course skip the Equinox log to prevent
					// recursion
					//

					if (reference == equinoxLogRef)
						return null;

					return super.addingService(reference);
				}

			};
			tracker.open();

			lrs.addLogListener((entry) -> {
				for (LogService normalLog : tracker.getTracked().values()) {

					int n = 0;
					if (entry.getServiceReference() != null)
						n += 1;
					if (entry.getException() != null)
						n += 2;
					switch (n) {
					case 0:
						normalLog.log(entry.getLevel(), entry.getMessage());
						break;

					case 1:
						normalLog.log(entry.getServiceReference(),
								entry.getLevel(), entry.getMessage());
						break;
					case 2:
						normalLog.log(entry.getLevel(), entry.getMessage(),
								entry.getException());
						break;
					case 3:
						normalLog.log(entry.getServiceReference(),
								entry.getLevel(), entry.getMessage(),
								entry.getException());
						break;
					}
				}
			});

		} catch (Error error) {
			// class loading error, no log service so Equinox cannot register
			// the log service, so we have no work
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

	@Override
	public void event(ServiceEvent event,
			Map<BundleContext, Collection<ListenerInfo>> listeners) {
		if (event.getServiceReference() == equinoxLogReaderRef
				|| event.getServiceReference() == equinoxLogRef)
			listeners.clear();
	}

	@Override
	public void find(BundleContext context, String name, String filter,
			boolean allServices, Collection<ServiceReference<?>> references) {
		references.remove(equinoxLogReaderRef);
		references.remove(equinoxLogRef);
	}

}
