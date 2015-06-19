package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import osgi.enroute.namespace.WebResourceNamespace;
import aQute.bnd.annotation.headers.RequireCapability;

/**
 * A Web Resource that provides Easse javascript files.
 */
@RequireCapability(ns = WebResourceNamespace.NS, filter = "(&(" + WebResourceNamespace.NS
		+ "=/osgi/enroute/easse)${frange;1.3.0})")
@Retention(RetentionPolicy.CLASS)
public @interface EasseWebResource {

	String[] resource() default {"easse.js","polyfill/eventsource.js"};

	int priority() default 100;
}
