package osgi.enroute.polymer.app.webresource.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import aQute.bnd.annotation.headers.RequireCapability;
import osgi.enroute.namespace.WebResourceNamespace;

/**
 * A sample web resource requirement 
 */

@RequireCapability(ns = WebResourceNamespace.NS, filter = "(&(" + WebResourceNamespace.NS + "="
		+ PolymerAppConstants.APPPATH + ")${frange;" + PolymerAppConstants.APPVERSION
		+ "})")
@Retention(RetentionPolicy.CLASS)
public @interface RequirePolymerAppWebresource {

	String[] resource() default "App.js";
	int priority() default 0;
}
