package osgi.enroute.executor.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.namespace.implementation.ImplementationNamespace;

import aQute.bnd.annotation.headers.RequireCapability;

/**
 */
@RequireCapability(ns = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, filter = "(&("
		+ ImplementationNamespace.IMPLEMENTATION_NAMESPACE
		+ "=" + ExecutorConstants.EXECUTOR_SPECIFICATION_NAME + ")${frange;${version;==;"
		+ ExecutorConstants.EXECUTOR_SPECIFICATION_VERSION + "}})")
@Retention(RetentionPolicy.CLASS)
public @interface RequireExecutorImplementation {
	/**
	 * The place where to look for resources
	 * 
	 * @return the location
	 */
	String configuration_loc() default "configuration/configuration.json";
}
