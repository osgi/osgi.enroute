package osgi.enroute.webserver.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.namespace.extender.ExtenderNamespace;

import aQute.bnd.annotation.headers.RequireCapability;

/**
 * Provide a facility to map entries in a bundle in the directory
 * {@code /static} to the local webserver.
 */
@RequireCapability(ns = ExtenderNamespace.EXTENDER_NAMESPACE, filter = "(&(" + ExtenderNamespace.EXTENDER_NAMESPACE
		+ "=osgi.enroute.webserver)${frange;" + WebServerConstants.WEB_SERVER_EXTENDER_VERSION + "})")
@Retention(RetentionPolicy.CLASS)
public @interface RequireWebServerExtender {}
