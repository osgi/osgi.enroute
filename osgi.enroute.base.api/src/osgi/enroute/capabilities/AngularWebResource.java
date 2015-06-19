package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import osgi.enroute.namespace.WebResourceNamespace;
import aQute.bnd.annotation.headers.RequireCapability;

/**
 * A Web Resource that provides Google's Angular JS javascript files.
 */
@RequireCapability(ns = WebResourceNamespace.NS, filter = "(&(" + WebResourceNamespace.NS
		+ "=/google/angular)${frange;1.3.8})")
@Retention(RetentionPolicy.CLASS)
public @interface AngularWebResource {

	String[] resource() default {"angular.js", "angular-route.js"};

	int priority() default 1000;
}
