package osgi.enroute.rest.simple.provider;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Servlet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.headers.ProvideCapability;
import osgi.enroute.dto.api.DTOs;
import osgi.enroute.http.capabilities.RequireHttpImplementation;
import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RestConstants;

/**
 * Making a REST service work is the result of the intersection between a
 * namespaced class that implements the {@code REST} interface, and a REST URI
 * service hook, which is created when a {@code UriMapper} is registered.
 * Mapping between the two is handed by the {@code UriMapper}.
 * 
 * This controller is responsible for listening to service registrations for
 * REST and UriMapper services, and instatiating the necessary resources (mostly
 * {@code RestServlet}s).
 */
@Designate(ocd=Config.class)
@RequireHttpImplementation
@ProvideCapability(ns = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, name = RestConstants.REST_SPECIFICATION_NAME, version = RestConstants.REST_SPECIFICATION_VERSION)
@Component(name = "osgi.enroute.rest.simple", immediate = true)
public class RestControllerService {
	Logger log = LoggerFactory.getLogger(RestControllerService.class);
	
	private static final String				DEFAULT_SERVLET_PATTERN	= "/rest/*";
	
	@Reference
	private DTOs							$dtos;
	private BundleContext					context;
	private final Map<String, RestServlet>	servlets				= new ConcurrentHashMap<>();
	private Config							config;
	private final String					defaultServletPattern[]	= new String[] {
			DEFAULT_SERVLET_PATTERN };
	private ServiceTracker<REST, REST>		tracker;

	@Activate
	void activate(BundleContext context, Config config) throws Exception {
		this.context = context;
		this.config = config;
		if (config.osgi_http_whiteboard_servlet_pattern() != null)
			this.defaultServletPattern[0] = config
					.osgi_http_whiteboard_servlet_pattern();

		log.trace(
				"Using default REST endpoint " + this.defaultServletPattern[0]);

		tracker = new ServiceTracker<REST, REST>(context, REST.class, null) {
			@Override
			public REST addingService(ServiceReference<REST> reference) {
				try {
					String[] namespaces = getNamespaces(reference);
					Integer ranking = (Integer) reference
							.getProperty(Constants.SERVICE_RANKING);
					if (ranking == null)
						ranking = new Integer(0);

					REST resourceManager = super.addingService(reference);
					
					for (String namespace : namespaces) {
						log.trace("adding REST %s on %s", resourceManager, namespace);
						RestServlet restServlet = servlets.computeIfAbsent(
								namespace,
								RestControllerService.this::createServlet);
						if (restServlet.closed.get()) {
						    reregister(restServlet, namespace);
						}
						restServlet.add(resourceManager, ranking);
					}
					return resourceManager;
				} catch (Exception e) {
					log.error("Failed to add rest endpoint {}", reference);
					return null;
				}
			}

			@Override
			public void removedService(ServiceReference<REST> reference,
					REST resourceManager) {
				try {
					String[] namespaces = getNamespaces(reference);
					for (String namespace : namespaces) {
						log.trace("removing REST {} on {}", resourceManager, namespace);
						RestServlet rs = servlets.get(namespace);
						rs.remove(resourceManager);
						rs.close();
						if (rs.count() == 0)
						    servlets.remove(rs);

						// we never clean them up. Seems to much work
						// since it is likely that the namespace is reused.
						// TODO or should we?
					}
					super.removedService(reference, resourceManager);
				} catch (Exception e) {
					log.error("Failed to remove rest endpoint {} from {}", resourceManager, reference);
				}
			}
		};
		tracker.open();
	}

	@Deactivate
	void deactivate() {
		tracker.close();
	}

	private String[] getNamespaces(ServiceReference<REST> reference)
			throws Exception {
		String namespaces[] = $dtos
				.convert(reference.getProperty(REST.ENDPOINT))
				.to(String[].class);

		if (namespaces == null || namespaces.length == 0)
			namespaces = defaultServletPattern;

		return namespaces;
	}

	private RestServlet createServlet(String namespace) {
		RestServlet rs = new RestServlet(config, namespace);
		Hashtable<String, Object> p = new Hashtable<>();
		p.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN,
				namespace);
		ServiceRegistration<Servlet> reg = context
				.registerService(Servlet.class, rs, p);
		rs.setCloseable(() -> reg.unregister());
		return rs;
	}

	private void reregister(RestServlet rs, String namespace) {
        Hashtable<String, Object> p = new Hashtable<>();
        p.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN,
                namespace);
        ServiceRegistration<Servlet> reg = context
                .registerService(Servlet.class, rs, p);
        rs.setCloseable(() -> reg.unregister());
        rs.closed.set(false);
	}
}
