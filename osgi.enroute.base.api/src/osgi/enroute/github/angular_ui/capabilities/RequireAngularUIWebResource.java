package osgi.enroute.github.angular_ui.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import aQute.bnd.annotation.headers.RequireCapability;
import osgi.enroute.namespace.WebResourceNamespace;

/**
 * A Web Resource that provides Angular-UI JS javascript files.
 */
@RequireCapability(ns = WebResourceNamespace.NS, filter = "(&(" + WebResourceNamespace.NS
 + "="
		+ AngularUIConstants.ANGULAR_UI_WEBRESOURCE_NAME + ")${frange;"+AngularUIConstants.ANGULAR_UI_WEBRESOURCE_VERSION+"})")
@Retention(RetentionPolicy.CLASS)
public @interface RequireAngularUIWebResource {

	/**
	 * Define the default resource to return
	 * 
	 * @return the list of resources to include
	 */
	String[]resource() default "ui-bootstrap-tpls.js";

	/**
	 * Define the priority of this web resources. The higher the priority, the
	 * earlier it is loaded when all web resources are combined.
	 * 
	 * @return the priority
	 */
	int priority() default 0;
}
