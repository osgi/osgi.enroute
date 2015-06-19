package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import osgi.enroute.namespace.WhiteboardNamespace;
import aQute.bnd.annotation.headers.RequireCapability;

/**
 * This is classic Http Service whiteboard as pioneered by Apache Felix.
 * Register under {@code javax.servlet.Servlet} class with service property
 * {@code alias=/<uri-prefix>}.
 */
@RequireCapability(ns = WhiteboardNamespace.NS, filter = "(&(" + WhiteboardNamespace.NS
		+ "=osgi.enroute.servlet)${frange;1.1.0})", effective = "active")
@Retention(RetentionPolicy.CLASS)
public @interface ServletWhiteboard {}
