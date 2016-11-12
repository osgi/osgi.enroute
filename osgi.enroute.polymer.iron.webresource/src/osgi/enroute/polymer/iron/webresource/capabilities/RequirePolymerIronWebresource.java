package osgi.enroute.polymer.iron.webresource.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import aQute.bnd.annotation.headers.RequireCapability;
import osgi.enroute.namespace.WebResourceNamespace;

/**
 * A sample web resource requirement 
 */

@RequireCapability(ns = WebResourceNamespace.NS, filter = "(&(" + WebResourceNamespace.NS + "="
		+ PolymerIronConstants.IRONPATH + ")${frange;" + PolymerIronConstants.IRONVERSION
		+ "})")
@Retention(RetentionPolicy.CLASS)
public @interface RequirePolymerIronWebresource {

	String[] resource() default "Iron.js";
	int priority() default 0;
}
