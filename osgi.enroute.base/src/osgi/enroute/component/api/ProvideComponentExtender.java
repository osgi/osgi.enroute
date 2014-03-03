package osgi.enroute.component.api;

import aQute.bnd.annotation.headers.ProvideCapability;
/**
 * An annotation that when applied to a type will create a capability for the 
 * OSGi component extender. This annotation should be used by implementations
 * of the OSGi Component specification.
 */
@ProvideCapability(ns = "osgi.extender", name = "osgi.component", version = "${@version}")
public @interface ProvideComponentExtender {
}
