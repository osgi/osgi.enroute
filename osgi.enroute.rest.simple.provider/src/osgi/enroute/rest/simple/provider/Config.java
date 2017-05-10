package osgi.enroute.rest.simple.provider;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
@interface Config {

    String osgi_http_whiteboard_servlet_pattern();

    /**
     * Configuration used to enable or disable CORS.
     */
    boolean corsEnabled() default false;

    /**
     * CORS header Access-Control-Allow-Origin
     */
    String allowOrigin() default "*";

    /**
     * CORS header Access-Control-Allow-Headers
     */
    String allowHeaders() default "Content-Type";

    /**
     * CORS Access-Control-Max-Age
     */
    int  maxAge() default 86400;

    /**
     * CORS Allow methods
     */
    String allowedMethods() default "GET, HEAD, POST, TRACE, OPTIONS";
}
