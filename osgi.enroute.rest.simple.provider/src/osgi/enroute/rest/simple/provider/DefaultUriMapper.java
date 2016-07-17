package osgi.enroute.rest.simple.provider;

import osgi.enroute.rest.api.UriMapper;

/**
 * The default is to return the default namespace (i.e. the empty string ""),
 * regardless of what the URI is.
 */
public class DefaultUriMapper implements UriMapper {
    @Override
    public String namespaceFor( String uri ) {
        return "";
    }
}
