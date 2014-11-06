package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import osgi.enroute.namespace.EndpointNamespace;
import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.bnd.annotation.headers.RequireCapability;

/**
 * This component provides a servlet that allows javascript clients to see the
 * Event Admin events.
 * <p>
 * The request path is treated as an EventAdmin topic filter, it is the
 * EVENT_TOPIC service property on an Event Handler service.
 * <p>
 * If the client registers with an {@code instance=<id>} then the request thread
 * can be killed from another request by specifying {@code abort=<id>}.
 * <p>
 * Event type = {@code org.osgi.service.eventadmin;topic=<topic>}. The data part
 * is a JSON string from the Event Admin properties that could be serialized to
 * JSON.
 */
public interface EventAdminSSEEndpoint {
	String	VERSION	= "1.0.0";
	String	NAME	= "/sse/1";
	String	NS		= EndpointNamespace.NS;

	@RequireCapability(ns = NS, filter = "(&(" + NS + "=" + NAME + ")${frange;" + VERSION + "})", effective = "active")
	@Retention(RetentionPolicy.CLASS)
	public @interface Require {

	}

	@ProvideCapability(ns = NS, name = NAME, version = VERSION, effective = "active")
	@Retention(RetentionPolicy.CLASS)
	public @interface Provide {

	}
}
