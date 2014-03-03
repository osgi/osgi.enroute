package osgi.enroute.component.api;

import aQute.bnd.annotation.headers.RequireCapability;
/**
 * An annotation that when applied to a type will create a requirement on the 
 * OSGi component extender. This annotation should be used by developers using
 * the OSGi Component specification.
 */
@RequireCapability(ns = "osgi.extender", filter = "(&(osgi.extender=osgi.component)${frange;${@version}})")
public @interface RequireComponentExtender {}
