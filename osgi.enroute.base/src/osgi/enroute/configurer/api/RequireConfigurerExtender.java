package osgi.enroute.configurer.api;

import aQute.bnd.annotation.headers.ProvideCapability;

/**
 * An annotation that should be applied to a types that provide the configurer.
 */
@ProvideCapability(ns="osgi.extender",name="osgi.configurer", version="${@version}")
public @interface RequireConfigurerExtender {}
