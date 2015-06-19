package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import osgi.enroute.namespace.WebResourceNamespace;
import aQute.bnd.annotation.headers.RequireCapability;

/**
 * A Web Resource that provides Jsonrpc javascript files.
 */
@RequireCapability(ns = WebResourceNamespace.NS, filter = "(&(" + WebResourceNamespace.NS
		+ "=/osgi/enroute/jsonrpc)${frange;1.1.1})")
@Retention(RetentionPolicy.CLASS)
public @interface JsonrpcWebResource {

	String[] resource() default {"jsonrpc.js"};

	int priority() default 100;
}
