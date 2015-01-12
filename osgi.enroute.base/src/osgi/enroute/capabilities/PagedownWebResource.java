package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import osgi.enroute.namespace.WebResourceNamespace;
import aQute.bnd.annotation.headers.RequireCapability;

/**
 * A Web Resource that provides Showdown markdown converter's javascript files.
 */
@RequireCapability(ns = WebResourceNamespace.NS, filter = "(&(" + WebResourceNamespace.NS
		+ "=/stackexchange/pagedown)${frange;1.1.1})")
@Retention(RetentionPolicy.CLASS)
public @interface PagedownWebResource {

	String[] resource() default "enmarkdown.js";

	int priority() default 0;
}
