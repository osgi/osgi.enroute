package osgi.enroute.rest.simple.provider;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
@interface Config {
    String org_enroute_rest_namespace() default "";

    boolean corsEnabled() default false;

    String osgi_http_whiteboard_servlet_pattern();
    
    //CORS header Access-Control-Allow-Origin
    String allowOrigin() default "*";
    //CORS header Access-Control-Allow-Methods
    String allowMethods() default "GET, POST, PUT";
    //CORS header Access-Control-Allow-Headers
    String allowHeaders() default "Content-Type";
    //CORS Access-Control-Max-Age
    int  maxAge() default 86400;
    //CORS Allow methods
    String allowedMethods() default "GET, HEAD, POST, TRACE, OPTIONS";
}
