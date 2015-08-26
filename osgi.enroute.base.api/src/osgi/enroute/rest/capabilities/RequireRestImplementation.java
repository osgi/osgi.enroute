package osgi.enroute.rest.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.namespace.implementation.ImplementationNamespace;

import aQute.bnd.annotation.headers.RequireCapability;

/**
 * Require an implementation for the this specification
 */
@RequireCapability(ns = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, filter = "(&("
		+ ImplementationNamespace.IMPLEMENTATION_NAMESPACE + "="
		+ RestConstants.LOGGER_SPECIFICATION_NAME + ")${frange;${version;==;"
		+ RestConstants.LOGGER_SPECIFICATION_VERSION + "}})")
@Retention(RetentionPolicy.CLASS)
public @interface RequireRestImplementation {}
