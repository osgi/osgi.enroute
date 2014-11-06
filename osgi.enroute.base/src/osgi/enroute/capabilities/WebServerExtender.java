package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.namespace.extender.ExtenderNamespace;

import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.bnd.annotation.headers.RequireCapability;

/**
 * Provide a facility to map entries in a bundle in the directory
 * {@code /static} to the local webserver.
 */
public interface WebServerExtender {
	String	VERSION	= "1.0.0";
	String	NAME	= "osgi.enroute.webserver";
	String	NS		= ExtenderNamespace.EXTENDER_NAMESPACE;

	@RequireCapability(ns = NS, filter = "(&(" + NS + "=" + NAME + ")${frange;" + VERSION + "})", effective = "active")
	@Retention(RetentionPolicy.CLASS)
	public @interface Require {

	}

	@ProvideCapability(ns = NS, name = NAME, version = VERSION, effective = "active")
	@Retention(RetentionPolicy.CLASS)
	public @interface Provide {

	}
}
