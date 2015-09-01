package osgi.enroute.namespace;

/**
 * A Web Resource is a bundle that contains resources that must be made
 * available on a web server. This Web Resource name space defines a mechanism
 * to include web resources in an application.
 * <p>
 * A web resource, for example Angular, must be wrapped in a bundle. The
 * preferred location is {@code /static/<owner>/<product>/<version>}. This
 * location will make the resource available over the web in a specific version.
 * This is however not required. For Angular this would be
 * {@code /static/google/angular/1.4.4}.
 * <p>
 * The web resource should then provide a capability. This capability name must
 * be the generic path, in the previous Angular example this would be {@code
 * /google/angular}. It should specify its version and a {@code root} attribute.
 * The root path must point to a folder in the bundle that contains the
 * resources. In our angular example this would be
 * {@code /static/google/angular/1.4.4} but it can be anywhere in the bundle.
 * The root path is <em>not</em> required to be publicly available though is is
 * recommended.
 * 
 * <pre>
 * Provide-Capability: \
 *   osgi.enroute.webresource; \
 *     osgi.enroute.webresource=/google/angular; \
 *     version:Version=1.4.4; \
 *     root=/static/google/angular/1.4.4
 * </pre>
 * 
 * Obviously macros should be used in bnd to remove the inevitable redundancy.
 * <p>
 * A bundle that wants to use a web resource should create a requirement to the
 * provided capability. For example:
 * 
 * <pre>
 * Require-Capability: \
 *   osgi.enroute.webresource; \
 *     filter:='(&(osgi.enroute.webresource=/google/angular)(version>=1.4.4)(!(version>=2.0.0)))';
 *     resource:List<String>="angular.js,angular-route.js,angular-resource.js";
 *     priority:Integer=1000
 * </pre>
 * 
 * The requirement can specify a {@code resource} and a {@code priority}
 * attribute. The resource attribute is a list if resources in the root folder
 * of the bundle that provides the web resource capability. The priority is used
 * to influence the order of include.
 * <p>
 * In runtime, the web server creates a virtual URI:
 * 
 * <pre>
 * 		{@code /osgi.enroute.webresource/<bsn>/<version>/<glob>}
 * </pre>
 * 
 * The {@code <bsn>}is the bundle symbolic name and the version is the
 * <em>exact</em> version of the bundle. The web server will find this bundles
 * and then look up all web resource wires from this bundle to any actual web
 * resources. It will then create a file that contains all the resources that
 * are listed by the requirements and that match the globbing pattern. The
 * priority will define the order, the higher, the earlier the resource is
 * loaded.
 * <p>
 * Additionally, any resources that match the globbing pattern in the requiring
 * bundle's {@code web} folder are added at the end. That is, applications
 * should place their own web resources that can be merged into one file in the
 * {@code /web} folder.
 * <p>
 * When building with bnd, macros can be used to synchronize the version and bsn
 * with the html file(s). For example:
 * 
 * <pre>
 * 	{@code <link} 
 * 	  rel="stylesheet" 
 * 	  type="text/css"
 * 	  href="/osgi.enroute.webresource/${bsn}/${Bundle-Version}/*.css">
 *  {@code <script} 
 * 	  src="/osgi.enroute.webresource/${bsn}/${Bundle-Version}/*.js">
 * 	</script>
 * </pre>
 * <p>
 * Adding these requirements of course is rather unpleasant and incredibly error
 * prone. It is therefore recommended that each web resource creates a
 * customized requirement annotation that can then be used by its clients. See
 * <a href="http://bnd.bndtools.org/chapters/230-manifest-annotations.html">
 * manifest annotations</a>. For example, in the Angular web resource this looks
 * like:
 * 
 * <pre>
 * &#64;RequireCapability(ns = WebResourceNamespace.NS, filter = "(&(osgi.enroute.webresource=/google/angular)${frange;1.4.4})")
 * &#64;Retention(RetentionPolicy.CLASS)
 * public @interface RequireAngularWebResource {
 * 	String[]resource() default {
 * 			"angular.js", "angular-route.js"
 * 	};
 * 
 * 	int priority() default 1000;
 * }
 * </pre>
 * 
 * This creates (when using bnd) the {@code RequireAngularWebResource}
 * annotation that, when applied anywhere in a bundle, will create the
 * aforementioned requirement.
 * <p>
 * This makes creating the requirement as simple as applying an annotation. In
 * general there is a single class that represents the application, this class
 * is quite well suited for this purpose. It is recommended that all web
 * resource requriements are placed on the same class.
 * 
 * <pre>
 * &#64;RequireAngular( resource={"angular.js", "angular-resource.js" ) public class
 * MyApplication { }
 * </pre>
 */
public @interface WebResourceNamespace {
	/**
	 * The namespace for webresources
	 */
	String NS = "osgi.enroute.webresource";
}
