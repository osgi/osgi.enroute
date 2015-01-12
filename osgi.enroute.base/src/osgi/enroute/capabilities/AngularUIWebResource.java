package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import osgi.enroute.namespace.WebResourceNamespace;
import aQute.bnd.annotation.headers.RequireCapability;

/**
 * A Web Resource that provides Angular-UI JS javascript files.
 */
@RequireCapability(ns = WebResourceNamespace.NS, filter = "(&(" + WebResourceNamespace.NS
		+ "=/github/angular-ui)${frange;0.12.0})")
@Retention(RetentionPolicy.CLASS)
public @interface AngularUIWebResource {

	String[] resource() default "ui-bootstrap-tpls.js";

	int priority() default 0;
}
