package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import aQute.bnd.annotation.headers.RequireCapability;
import osgi.enroute.namespace.WebResourceNamespace;

/**
 * A Web Resource that provides Twitter's Bootstrap files.
 */
@RequireCapability(ns = WebResourceNamespace.NS, filter = "(&(" + WebResourceNamespace.NS
		+ "=/twitter/bootstrap)${frange;3.3.1})")
@Retention(RetentionPolicy.CLASS)
public @interface BootstrapWebResource {

	/**
	 * Define the default resource to return
	 * 
	 * @return the list of resources to include
	 */
	String[] resource() default "bootstrap.css";

	/**
	 * Define the priority of this web resources. The higher the priority, the
	 * earlier it is loaded when all web resources are combined.
	 * 
	 * @return the priority
	 */
	int priority() default 1000;
}
