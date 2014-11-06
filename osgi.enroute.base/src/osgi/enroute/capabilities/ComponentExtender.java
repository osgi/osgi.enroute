package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.namespace.extender.ExtenderNamespace;

import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.bnd.annotation.headers.RequireCapability;

/**
 * Require a DS/SCR to be available.
 */
public interface ComponentExtender {
	String	VERSION	= "1.2.1";
	String	NAME	= "osgi.component";
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
