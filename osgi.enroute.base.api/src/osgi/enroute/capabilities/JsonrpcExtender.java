package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.namespace.extender.ExtenderNamespace;

import aQute.bnd.annotation.headers.RequireCapability;

/**
 * Require a JSON RPC to be available.
 */
@RequireCapability(ns = ExtenderNamespace.EXTENDER_NAMESPACE, filter = "(&(" + ExtenderNamespace.EXTENDER_NAMESPACE + "=osgi.enroute.jsonrpc)${frange;1.1.1})", effective = "active")
@Retention(RetentionPolicy.CLASS)
public @interface JsonrpcExtender {
}
