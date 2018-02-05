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
}
