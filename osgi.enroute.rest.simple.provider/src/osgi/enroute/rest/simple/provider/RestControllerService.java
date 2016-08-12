package osgi.enroute.rest.simple.provider;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Servlet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
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
@RequireHttpImplementation
@ProvideCapability(ns = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, name = RestConstants.REST_SPECIFICATION_NAME, version = RestConstants.REST_SPECIFICATION_VERSION)
@Component(name = "osgi.enroute.rest.simple", immediate = true)
public class RestControllerService {
	private static final String				DEFAULT_SERVLET_PATTERN	= "/rest/*";
	private static Logger					log						= LoggerFactory
			.getLogger(RestControllerService.class);
	@Reference
	private DTOs							dtos;
	private BundleContext					context;
	private final Map<String, RestServlet>	servlets				= new ConcurrentHashMap<>();
	private Config							config;
	private final String					defaultServletPattern[]	= new String[] {
			DEFAULT_SERVLET_PATTERN };

	@Activate
	void activate(BundleContext context, Config config) throws Exception {
		this.context = context;
		this.config = config;
		if (config.osgi_http_whiteboard_servlet_pattern() != null)
			this.defaultServletPattern[0] = config
					.osgi_http_whiteboard_servlet_pattern();

		log.trace(
				"Using default REST endpoint " + this.defaultServletPattern[0]);
	}

	void deactivate() {
		for (Iterator<RestServlet> i = servlets.values().iterator(); i
				.hasNext();) {
			RestServlet r = i.next();
			try {
				i.remove();
				r.close();
			} catch (IOException e) {
				log.warn("deactivate: closing RESTServlet " + r, e);
			}
		}
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addREST(REST resourceManager, Map<String, Object> properties)
			throws Exception {
		String[] namespaces = getNamespaces(properties);
		Integer ranking = (Integer) properties.get(Constants.SERVICE_RANKING);
		if (ranking == null)
			ranking = new Integer(0);

		for (String namespace : namespaces) {
			RestServlet restServlet = servlets.computeIfAbsent(namespace,
					this::createServlet);
			restServlet.add(resourceManager, ranking);
		}
	}

	synchronized void removeREST(REST resourceManager,
			Map<String, Object> properties) throws Exception {
		String[] namespaces = getNamespaces(properties);
		for (String namespace : namespaces) {
			RestServlet rs = servlets.get(namespace);
			rs.remove(resourceManager);
			
			// we never clean them up. Seems to much work
			// since it is likely that the namespace is reused.
			// TODO or should we?
		}
	}

	private String[] getNamespaces(Map<String, Object> properties)
			throws Exception {
		String namespaces[] = dtos.convert(properties.get(REST.ENDPOINT))
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

}
