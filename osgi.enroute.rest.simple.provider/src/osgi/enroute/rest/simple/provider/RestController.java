package osgi.enroute.rest.simple.provider;

import java.util.List;

import osgi.enroute.rest.api.UriMapper;

public interface RestController {
    List<UriMapper> uriMappersFor(String servletPattern);
    RestMapper restMapperFor(String namespace);
}
