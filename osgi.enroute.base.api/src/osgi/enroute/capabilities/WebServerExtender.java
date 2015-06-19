package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.namespace.extender.ExtenderNamespace;

import aQute.bnd.annotation.headers.RequireCapability;

/**
 * Provide a facility to map entries in a bundle in the directory
 * {@code /static} to the local webserver.
 */
@RequireCapability(ns = ExtenderNamespace.EXTENDER_NAMESPACE, filter = "(&(" + ExtenderNamespace.EXTENDER_NAMESPACE
		+ "=osgi.enroute.webserver)${frange;1.1.0})", effective = "active")
@Retention(RetentionPolicy.CLASS)
public @interface WebServerExtender {}
