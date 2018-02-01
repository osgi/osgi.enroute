package osgi.enroute.rest.simple.provider;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
@interface Config {

    String osgi_http_whiteboard_servlet_pattern();

    /**
     * By default, all REST calls require SSL/TLS. For backwards compatibility, this
     * requirement can be removed by setting this field to "false".
     */
    boolean requireSSL() default true;

    /**
     * By default, if a non-secure request is made (i.e. the resource https://example.com/resource is
     * requested via http://example.com/resource), a 404 error is returned. To return a different
     * error, set this field to the http status code value.
     */
    int notSecureError() default 404;

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
