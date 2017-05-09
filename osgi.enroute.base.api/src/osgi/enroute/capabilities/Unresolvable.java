package osgi.enroute.capabilities;

import aQute.bnd.annotation.headers.RequireCapability;

/**
 * The purpose of this capability is to be never satisfied. This can be required
 * by bundles that are compile only or class path only.
 */
@RequireCapability(ns = "osgi.unresolvable", filter = "(&(must.not.resolve=*)(!(must.not.resolve=*)))")
public @interface Unresolvable {

}
