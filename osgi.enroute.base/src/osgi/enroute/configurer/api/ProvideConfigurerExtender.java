package osgi.enroute.configurer.api;

import aQute.bnd.annotation.headers.RequireCapability;

/**
 * An annotation that should be applied to a types that need the configurer to be present.
 */
@RequireCapability(ns="osgi.extender",filter="((osgi.extender=osgi.enroute.configurer)${frange;${@version}})")
public @interface ProvideConfigurerExtender {}
