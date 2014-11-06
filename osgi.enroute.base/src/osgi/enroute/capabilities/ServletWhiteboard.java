package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import osgi.enroute.namespace.WhiteboardNamespace;
import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.bnd.annotation.headers.RequireCapability;

/**
 * This is classic Http Service whiteboard as pioneered by Apache Felix.
 * Register under {@code javax.servlet.Servlet} class with service property
 * {@code alias=/<uri-prefix>}.
 */
public interface ServletWhiteboard {
	String	VERSION	= "1.0.0";
	String	NAME	= "osgi.enroute.servlet";
	String	NS		= WhiteboardNamespace.NS;

	@RequireCapability(ns = NS, filter = "(&(" + NS + "=" + NAME + ")${frange;" + VERSION + "})", effective = "active")
	@Retention(RetentionPolicy.CLASS)
	public @interface Require {

	}

	@ProvideCapability(ns = NS, name = NAME, version = VERSION, effective = "active")
	@Retention(RetentionPolicy.CLASS)
	public @interface Provide {

	}
}
