package osgi.enroute.webcomponentsjs.webresource.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import aQute.bnd.annotation.headers.RequireCapability;
import osgi.enroute.namespace.WebResourceNamespace;

/**
 * A sample web resource requirement 
 */

@RequireCapability(ns = WebResourceNamespace.NS, filter = "(&(" + WebResourceNamespace.NS + "="
		+ WebcomponentsJSConstants.WEBCOMPONENTSJSPATH + ")${frange;" + WebcomponentsJSConstants.WEBCOMPONENTSJSVERSION
		+ "})")
@Retention(RetentionPolicy.CLASS)
public @interface RequireWebcomponentsJSWebresource {

	String[] resource() default "Webcomponentsjs.js";
	int priority() default 0;
}
