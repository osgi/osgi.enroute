package osgi.enroute.rest.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to provide public information about a {@link REST} API.
 * 
 * Annotated functions will be parsed and the information displayed when calling 
 * /inf relative to the REST endpoint.
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Inf {
    String value();
}
