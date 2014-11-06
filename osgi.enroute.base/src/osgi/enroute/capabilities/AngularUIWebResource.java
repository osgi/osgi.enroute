package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import osgi.enroute.namespace.WebResourceNamespace;
import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.bnd.annotation.headers.RequireCapability;

/**
 * A Web Resource that provides Google's Angular JS javascript files under
 * {@value #NAME} as laid out in their ZIP file. That is, the {@value #NAME}
 * {@code /angular.js} file must be available.
 */
public interface AngularUIWebResource {
	String	VERSION	= "0.11.2";
	String	NAME	= "/github/angular-ui/0";
	String	NS		= WebResourceNamespace.NS;

	@RequireCapability(ns = NS, filter = "(&(" + NS + "=" + NAME + ")${frange;" + VERSION + "})", effective = "active")
	@Retention(RetentionPolicy.CLASS)
	public @interface Require {}

	@ProvideCapability(ns = NS, name = NAME, version = VERSION, effective = "active")
	@Retention(RetentionPolicy.CLASS)
	public @interface Provide {}
}
