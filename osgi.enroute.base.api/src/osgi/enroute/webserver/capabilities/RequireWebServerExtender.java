package osgi.enroute.webserver.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.namespace.extender.ExtenderNamespace;

import aQute.bnd.annotation.headers.RequireCapability;
import osgi.enroute.namespace.WebResourceNamespace;

/**
 * A web server extender must map static resources in the {@code /static}
 * directory to the web, using the remaining path. For example, a resource
 * {@code /static/foo/bar/baz.png} must be available under
 * {@code /foo/bar/baz.png} on the local web server. Since this space is shared,
 * it is strongly recommended to paths do not clash.
 * <p>
 * The extender must overlay all resources in bundle ID order. That is, multiple
 * bundles can contribute to the same folder and the bundle with the highest ID
 * will overwrite bundles with the same resource paths but a lower bundle id.
 * <p>
 * Web Extenders must also {@link WebResourceNamespace} resources.
 * <p>
 * Web extenders must also provide support for common web standards like ranges,
 * zipping, caching, etc.
 */
@RequireCapability(ns = ExtenderNamespace.EXTENDER_NAMESPACE, filter = "(&(" + ExtenderNamespace.EXTENDER_NAMESPACE
		+ "=osgi.enroute.webserver)${frange;" + WebServerConstants.WEB_SERVER_EXTENDER_VERSION + "})")
@Retention(RetentionPolicy.CLASS)
public @interface RequireWebServerExtender {}
