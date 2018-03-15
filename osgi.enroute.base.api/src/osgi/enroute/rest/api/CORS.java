package osgi.enroute.rest.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable an HTTP method of a REST implementation for cross-scripting. To apply
 * CORS to a specific REST endpoint, annotate the endpoint method in the REST
 * implementation with this annotation. To apply CORS to all endpoints in a REST
 * implementation, annotate the REST class. If both are applied, method
 * annotations (when existing) take precedence over the class annotation.
 * Dynamic resolution of the allowed origin, allowed methods, and allowed
 * headers can be provided by implementing a method within the REST
 * implementation class. The method has the naming scheme
 * <code>config ([+ method] + impl) [+cardinality]</code> where
 * <code>config = allowOrigin | allowMethods | allowHeaders</code>,
 * <code>method</code> is the HTTP method, <code>impl</code> is the name of the
 * REST implementation method, <code>cardinality</code> is the number of
 * arguments (see REST implementation). Use <code>All</code> to match all
 * methods. The method name is written in camel case. Dynamic value resolution
 * is attempted at runtime using the following algorithm:
 * <ol>
 * <ul>
 * Look for the most specific method. Example: allowOriginPostUser0()
 * </ul>
 * <ul>
 * Look for the most specific method excluding cardinality. Example:
 * allowOriginPostUser()
 * </ul>
 * <ul>
 * Look for the most specific method for any HTTP method. Example:
 * allowOriginAllUser0()
 * </ul>
 * <ul>
 * Look for the most specific method for any HTTP method, excluding cardinality.
 * Example: allowOriginAllUser()
 * </ul>
 * <ul>
 * Look for the catch-all method. Example: allowOrigin()
 * </ul>
 * </ol>
 * <code>allowOrigin</code> must have the signature
 * <code>java.util.function.Function<String, Optional<String>> allowOrigin[method][impl][cardinality]()</code>
 * as specified above. <code>allowMethods</code> must have the signature
 * <code>java.util.function.Function<String, String> allowMethods[method][impl][cardinality]()</code>
 * as specified as described above. <code>allowHeaders</code> must have the
 * signature
 * <code>BiFunction<String, List<String>, String> allowHeaders[method][impl][cardinality]()</code>
 * as specified as described above. Example: <code>
 * java.util.Function<String, Optional<String>> allowOrigin() {
 *   return origin -> {
 *     ... // dynamic code here
 *   };
 * }
 * </code>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({
		ElementType.TYPE, ElementType.METHOD
})
public @interface CORS {

	/**
	 * The marker value to indicate use of dynamic value resolution.
	 */
	public static final String	DYNAMIC	= "DYNAMIC";

	/**
	 * Accept all origins or headers.
	 */
	public static final String	ALL		= "*";

	/**
	 * The value to use for the Access-Control-Allow-Origin header. Set as
	 * "DYNAMIC" to use dynamic resolution. For security reasons, by default no
	 * origins are accepted. To accept an origin this value must be configured.
	 */
	String origin() default "";

	/**
	 * The value to use for the Access-Control-Allow-Methods header. Set as a
	 * single value "DYNAMIC" to use dynamic resolution. When the value is
	 * static, the provided allowed methods will be used for all origins. When
	 * the value "*" is provided, the allowed methods will be the same as those
	 * provided in the Allow header (i.e. all methods are allowed) for all
	 * origins. To provide per-origin allowed methods, use dynamic resolution.
	 * For security purposes, by default no methods are accepted.
	 */
	String[] allowMethods() default {};

	/**
	 * The value to use for the Access-Control-Allow-Headers header. Set as a
	 * single value "DYNAMIC" to use dynamic resolution.
	 */
	String[] allowHeaders() default {
			"Content-Type"
	};

	/**
	 * Determines whether or not the Access-Control-Allow-Credentials header
	 * gets set. When configured to {@code true}, the CORS header will be set to
	 * "true". Otherwise, the header is not set.
	 */
	boolean allowCredentials() default false;

	/**
	 * Custom headers to expose to the cross-site requestor. If the list is not
	 * empty, sets the CORS header Access-Control-Expose-Headers to the values
	 * provided.
	 */
	String[] exposeHeaders() default {
			"X-Powered-By"
	};

	/**
	 * Sets the Access-Control-Max-Age to the value provided.
	 */
	int maxAge() default 86400;
}
