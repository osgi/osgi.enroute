package osgi.enroute.angular.web;

import aQute.bnd.annotation.headers.RequireCapability;

@RequireCapability(ns="osgi.webresource", filter="(&(osgi.webresource=/google/angular)${frange;${@version}})")
public @interface Angular {
}
