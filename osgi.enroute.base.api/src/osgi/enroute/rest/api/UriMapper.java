package osgi.enroute.rest.api;

/**
 * Used to map a request URI to a REST namespace.
 * 
 * Register a service that implements this interface in order to configure
 * a REST URI service hook. That is, expose a URI that will either be a base URI
 * for several endpoints, or an endpoint in itself.
 * 
 * The path of the servlet that is instantiated behind the scenes
 * will have the servlet path provided by this service configuration (using the
 * {@code HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN} configuration property.
 * Multiple {@code UriMapper}s can be registred against the same servlet pattern. Priority
 * is determined by setting the {@code Constants.SERVICE_RANKING} property.
 */
@FunctionalInterface
public interface UriMapper {
    /**
     * Returns the REST namespace associated to the provided URI.
     * 
     * If the mapping is out-of-scope, then {@code null} is returned, and the
     * next highest {@code UriMapper} will be tried. If no mappers are in scope,
     * finally the default namespace (empty string) will be used. 
     * 
     * @param uri the URI against which the REST namespace is mapped
     * @return the namespce mapped to the URI. An empty string is the default
     *         namespace.
     */
    String namespaceFor(String uri);
}
