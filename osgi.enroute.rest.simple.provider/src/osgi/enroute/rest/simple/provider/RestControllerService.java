package osgi.enroute.rest.simple.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.osgi.framework.Constants;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.headers.ProvideCapability;
import osgi.enroute.http.capabilities.RequireHttpImplementation;
import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RestConstants;
import osgi.enroute.rest.api.UriMapper;

/**
 * Making a REST service work is the result of the intersection between a
 * namespaced class that implements the {@code REST} interface, and a
 * REST URI service hook, which is created when a {@code UriMapper} is registered.
 * Mapping between the two is handed by the {@code UriMapper}.
 * 
 * This controller is responsible for listening to service registrations for
 * REST and UriMapper services, and instatiating the necessary resources
 * (mostly {@code RestServlet}s).
 */
@RequireHttpImplementation
@ProvideCapability(
        ns=ImplementationNamespace.IMPLEMENTATION_NAMESPACE, 
        name=RestConstants.REST_SPECIFICATION_NAME, 
        version=RestConstants.REST_SPECIFICATION_VERSION)
@Component(
        service = RestController.class,
        name = "osgi.enroute.rest.simple",
        immediate = true
    )
public class RestControllerService implements RestController {
    static final String NAMESPACE_PARAM = "org.enroute.rest.namespace";

    static final String DEFAULT_NAMESPACE = "";
    static final String DEFAULT_SERVLET_PATTERN = "/rest/*";

    private final Map<String, Set<REST>> resourceManagers = new HashMap<>();
    private final Map<String, RestMapper> restMappers = new HashMap<>();
    private final Map<String, TreeMap<Integer, UriMapper>> uriMappers = new HashMap<>();
    private final List<PendingMapper> pendingMappers = new ArrayList<>();

    @Reference private ConfigurationAdmin cm;
    @Reference private LogService log;

    boolean isActivated = false;

    @Activate
    void activate(Map<String, Object> properties) throws Exception {
        // Add the default namespace
        resourceManagers.put(DEFAULT_NAMESPACE, new HashSet<>());
        restMappers.put(DEFAULT_NAMESPACE, new RestMapper());
        TreeMap<Integer, UriMapper> uriMapperMap = new TreeMap<>();
        uriMapperMap.put(0, s -> "");
        uriMappers.put(DEFAULT_SERVLET_PATTERN, uriMapperMap);
        startServlet(DEFAULT_SERVLET_PATTERN, properties);
        isActivated = true;

        instantiatePendingMappers();
    }

    void deactivate() {
        isActivated = false;
        stopServlet(DEFAULT_SERVLET_PATTERN);
        uriMappers.clear();
        restMappers.clear();
        resourceManagers.clear();
    }

    @Override
    public List<UriMapper> uriMappersFor(String servletPattern) {
        return uriMappers.get( servletPattern )
            .values()
            .stream()
            .collect( Collectors.toList() );
    }

    @Override
    public RestMapper restMapperFor(String namespace) {
        return restMappers.get(namespace);
    }

    @Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
    synchronized void addREST(REST resourceManager, Map<String, String> properties) {
        String namespace = properties.get(NAMESPACE_PARAM);
        if(namespace == null)
            namespace = "";
        if(!resourceManagers.containsKey(namespace)) {
            resourceManagers.put(namespace, new HashSet<>());
            restMappers.put(namespace, new RestMapper());
        }
        RestMapper restMapper = restMappers.get(namespace);
        restMapper.addResource(resourceManager);

        Set<REST> resourceManagerSet = resourceManagers.get(namespace);
        resourceManagerSet.add(resourceManager);
    }

    synchronized void removeREST(REST resourceManager, Map<String, String> properties) {
        String namespace = properties.get(NAMESPACE_PARAM);
        if(namespace == null)
            namespace = "";

        Set<REST> resourceManagerSet = resourceManagers.get(namespace);
        resourceManagerSet.remove(resourceManager);

        if(!namespace.isEmpty() && resourceManagers.containsKey(namespace) && resourceManagers.get(namespace).isEmpty()) {            
            restMappers.remove(namespace);
            resourceManagers.remove(namespace);
        }
    }

    @Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
    synchronized void addUriMapper(UriMapper mapper, Map<String, Object> properties) throws Exception {
        if( !isActivated )
            pendingMappers.add(new PendingMapper(mapper, properties));
        else
            instantiateMapper( mapper, properties );
    }

    synchronized void removeUriMapper(UriMapper mapper, Map<String, String> properties) {
        String name = properties.get(Constants.SERVICE_PID);
        if(name != null) {
            String pattern = properties.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN);
            if(pattern == null || pattern.isEmpty())
                pattern = DEFAULT_SERVLET_PATTERN;
            Map<Integer, UriMapper> mappersForPattern = uriMappers.get(pattern);
            mappersForPattern.remove(mapper);
            if(mappersForPattern.isEmpty())
                stopServlet(pattern);
        }
    }

    private void instantiatePendingMappers() throws Exception {
        for( final PendingMapper mapper : pendingMappers )
            instantiateMapper( mapper.mapper, mapper.properties );

        pendingMappers.clear();
    }

    private void instantiateMapper(UriMapper mapper, Map<String, Object> properties) throws Exception {
        String name = (String)properties.get(Constants.SERVICE_PID);
        if(name != null) {
            String pattern = (String)properties.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN);
            if(pattern == null || pattern.isEmpty())
                pattern = DEFAULT_SERVLET_PATTERN;

            if(!uriMappers.containsKey(pattern)) {
                uriMappers.put(pattern, new TreeMap<>());
                startServlet(pattern, properties);
            }

            Integer ranking = (Integer)properties.get(Constants.SERVICE_RANKING);
            if(ranking == null)
                ranking = 0;
            TreeMap<Integer, UriMapper> mappersForPattern = uriMappers.get(pattern);
            mappersForPattern.put(ranking, mapper);
            uriMappers.put(pattern, new TreeMap<>(mappersForPattern.descendingMap()));
        }
    }

    private void startServlet(String pattern, Map<String, Object> properties) throws Exception {
        Configuration configuration = cm.createFactoryConfiguration("osgi.enroute.rest.simple.servlet", "?");
        Dictionary<String, Object> d = new Hashtable<>();
        d.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, pattern);
        // Propagate the properties we want.
        // This way is no goo, though. There must be a better way to do this!!
        Set<String> toCopy = new HashSet<>(Arrays.asList( new String[]{
                "corsEnabled", "allowOrigin", "allowMethods", "allowHeaders", "maxAge", "allowedMethods"}));
        copyProperties( properties, d, toCopy );
        configuration.update(d);
    }

    private void copyProperties(Map<String, Object> from, Dictionary<String, Object> to, Set<String> properties) {
        for(String property : properties) {
            if(from.containsKey(property))
                to.put(property, from.get(property));
        }
    }
    private void stopServlet(String pattern) {
        // How do I do this?
    }

    private static class PendingMapper {
        UriMapper mapper;
        Map<String, Object> properties;

        public PendingMapper( UriMapper mapper, Map<String, Object> properties ) {
            this.mapper = mapper;
            this.properties = new HashMap<>(properties);
        }
    }
}
