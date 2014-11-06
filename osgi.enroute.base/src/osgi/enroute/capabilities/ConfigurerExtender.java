package osgi.enroute.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.namespace.extender.ExtenderNamespace;

import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.bnd.annotation.headers.RequireCapability;

/**
 * This an extender that reads {@value #CONFIGURATION_LOC} file. These files are
 * JSON formatted, though they do support comments. Additionally, if the
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
 * The format of the JSON file is mapped to
 * {@code List<Hashtable<String,Object>>}.
 * <p>
 * If a factory is used, then the {@code service.pid} is logical. That is, if a
 * factory instance already exists with that name then the data is updated,
 * otherwise a new record is created.
 */
public interface ConfigurerExtender {
	String	VERSION				= "1.0.0";
	String	NAME				= "osgi.enroute.configurer";
	String	NS					= ExtenderNamespace.EXTENDER_NAMESPACE;
	String	CONFIGURATION_LOC	= "configuration/configuration.json";

	@RequireCapability(ns = NS, filter = "(&(" + NS + "=" + NAME + ")${frange;" + VERSION + "})", effective = "active")
	@Retention(RetentionPolicy.CLASS)
	public @interface Require {

	}

	@ProvideCapability(ns = NS, name = NAME, version = VERSION, effective = "active")
	@Retention(RetentionPolicy.CLASS)
	public @interface Provide {

	}
}
