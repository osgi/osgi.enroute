package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import aQute.bnd.annotation.headers.RequireCapability;
import osgi.enroute.namespace.WebResourceNamespace;

/**
 * A Web Resource that provides Easse javascript files.
 */
@RequireCapability(ns = WebResourceNamespace.NS, filter = "(&(" + WebResourceNamespace.NS
		+ "=/osgi/enroute/easse)${frange;1.3.0})")
@Retention(RetentionPolicy.CLASS)
public @interface EasseWebResource {

	/**
	 * Define the default resource to return
	 * 
	 * @return the list of resources to include
	 */
	String[] resource() default {"easse.js","polyfill/eventsource.js"};

	/**
	 * Define the priority of this web resources. The higher the priority, the
	 * earlier it is loaded when all web resources are combined.
	 * 
	 * @return the priority
	 */
	int priority() default 100;
}
