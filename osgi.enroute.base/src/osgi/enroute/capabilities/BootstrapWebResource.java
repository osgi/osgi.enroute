package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import osgi.enroute.namespace.WebResourceNamespace;
import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.bnd.annotation.headers.RequireCapability;

/**
 * A Web Resource that provides Twitter's Bootstrap files under {@value #NAME}
 * as outlayed in their zip file. That is, the {@value #NAME} {@code /css}
 * directory (and the others) must be available.
 */
public interface BootstrapWebResource {
	String	VERSION	= "3.2.0";
	String	NAME	= "/twitter/bootstrap/3";
	String	NS		= WebResourceNamespace.NS;

	@RequireCapability(ns = NS, filter = "(&(" + NS + "=" + NAME + ")${frange;" + VERSION + "})", effective = "active")
	@Retention(RetentionPolicy.CLASS)
	public @interface Require {

	}

	@ProvideCapability(ns = NS, name = NAME, version = VERSION, effective = "active")
	@Retention(RetentionPolicy.CLASS)
	public @interface Provide {

	}
}
