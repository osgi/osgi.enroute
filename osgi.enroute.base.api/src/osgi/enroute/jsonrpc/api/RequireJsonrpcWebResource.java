package osgi.enroute.jsonrpc.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import aQute.bnd.annotation.headers.RequireCapability;
import osgi.enroute.namespace.WebResourceNamespace;

/**
 * A Web Resource that provides Jsonrpc javascript files.
 */
@RequireCapability(ns = WebResourceNamespace.NS, filter = "(&(" + WebResourceNamespace.NS
 + "="
		+ JsonrpcConstants.JSONRPC_WEB_RESOURCE_PATH + ")${frange;" + JsonrpcConstants.JSONRPC_WEB_RESOURCE_VERSION
		+ "})")
@Retention(RetentionPolicy.CLASS)
public @interface RequireJsonrpcWebResource {

	/**
	 * Define the default resource to return
	 * 
	 * @return the list of resources to include
	 */
	String[] resource() default {"jsonrpc.js"};

	/**
	 * Define the priority of this web resources. The higher the priority, the
	 * earlier it is loaded when all web resources are combined.
	 * 
	 * @return the priority
	 */
	int priority() default 100;
}
