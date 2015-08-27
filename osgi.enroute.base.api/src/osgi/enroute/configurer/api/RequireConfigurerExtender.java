package osgi.enroute.configurer.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.namespace.extender.ExtenderNamespace;

import aQute.bnd.annotation.headers.RequireCapability;

/**
 * This an extender that reads {@link #configuration_loc()} file. These files
 * are JSON formatted, though they do support comments. Additionally, if the
 * {@code enRoute.configurer} System property is set, it is also read as a
 * configuration file.
 * <p>
 * Macros are fully supported. The variable order is configuration, system
 * properties, settings in ~/.enRoute/settings.json.
 * <p>
 * The configurer can also refer to binary resources with @{resource-path}. This
 * pattern requires a resource path inside the bundle. This resource is copied
 * to the local file system and the macro is replaced with the corresponding
 * path.
 * <p>
 * The format of the JSON file is mapped to {@code List<Hashtable
 * <String,Object>>}.
 * <p>
 * If a factory is used, then the {@code service.pid} is logical. That is, if a
 * factory instance already exists with that name then the data is updated,
 * otherwise a new record is created.
 */
@RequireCapability(ns = ExtenderNamespace.EXTENDER_NAMESPACE, filter = "(&(" + ExtenderNamespace.EXTENDER_NAMESPACE
		+ "=" + ConfigurerConstants.CONFIGURER_EXTENDER_NAME + ")${frange;"
		+ ConfigurerConstants.CONFIGURER_EXTENDER_VERSION + "})")
@Retention(RetentionPolicy.CLASS)
public @interface RequireConfigurerExtender {
	/**
	 * The place where to look for resources
	 * 
	 * @return the location
	 */
	String configuration_loc() default "configuration/configuration.json";
}
