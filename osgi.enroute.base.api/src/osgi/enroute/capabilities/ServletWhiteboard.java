package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.namespace.implementation.ImplementationNamespace;

import aQute.bnd.annotation.headers.RequireCapability;

/**
 * This is classic Http Service whiteboard as pioneered by Apache Felix.
 * Register under {@code javax.servlet.Servlet} class with service property
 * {@code alias=/<uri-prefix>}.
 */
@RequireCapability(ns = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, filter = "(&("
		+ ImplementationNamespace.IMPLEMENTATION_NAMESPACE
		+ "=osgi.http)${frange;1.0.0})")
@Retention(RetentionPolicy.CLASS)
public @interface ServletWhiteboard {}
