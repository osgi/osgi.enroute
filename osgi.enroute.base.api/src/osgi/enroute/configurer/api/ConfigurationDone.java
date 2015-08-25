package osgi.enroute.configurer.api;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The purpose of this service is to signal that the configuration for all
 * initially installed bundles is done. Making sure that this class is a
 * dependency will make the startup go smoother since no bundles will have to
 * reinitialize because they receive configuration.
 * <p>
 * This service is registered under its actual name as well as Object to limit
 * the type dependencies. It will have a property
 * {@code configuration.done=true} that can be used in its target filter.
 */
@ProviderType
public interface ConfigurationDone {
	/**
	 * Property registered when the configuration is done
	 */
	String	CONFIGURATION_DONE		= "configuration.done";
	/**
	 * TODO
	 */
	String	BUNDLE_CONFIGURATION	= "Bundle-Configuration";
}
