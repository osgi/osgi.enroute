package osgi.enroute.rest.api;

@FunctionalInterface
public interface UriMapper {
    String namespaceFor(String uri);
}
