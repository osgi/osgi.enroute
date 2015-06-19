package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import osgi.enroute.namespace.WebResourceNamespace;
import aQute.bnd.annotation.headers.RequireCapability;

/**
 * A Web Resource that provides Twitter's Bootstrap files.
 */
@RequireCapability(ns = WebResourceNamespace.NS, filter = "(&(" + WebResourceNamespace.NS
		+ "=/twitter/bootstrap)${frange;3.3.1})")
@Retention(RetentionPolicy.CLASS)
public @interface BootstrapWebResource {

	String[] resource() default "bootstrap.css";

	int priority() default 1000;
}
