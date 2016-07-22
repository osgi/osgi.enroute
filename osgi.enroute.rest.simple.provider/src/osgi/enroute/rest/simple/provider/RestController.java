package osgi.enroute.rest.simple.provider;

import java.util.List;

import osgi.enroute.rest.api.UriMapper;

/**
 * This service is only used locally. See {@code RestControllerService} for details. 
 */
public interface RestController {
    List<UriMapper> uriMappersFor(String servletPattern);
    RestMapper restMapperFor(String namespace);
}
