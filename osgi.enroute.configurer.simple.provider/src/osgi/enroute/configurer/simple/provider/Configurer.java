package osgi.enroute.configurer.simple.provider;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;

import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.lib.collections.ExtList;
import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.lib.settings.Settings;
import aQute.libg.sed.ReplacerAdapter;
import osgi.enroute.configurer.api.ConfigurationDone;
import osgi.enroute.configurer.api.ConfigurerConstants;

/**
 * This component is an extender that reads {@link #CONFIGURATION_LOC} file.
 * These files are JSON formatted, though they do support comments.
 * Additionally, if the enRoute.configurer System property is set, it is also
 * read.
 * <p>
 * This configurer also supports profiles. The default profile is "debug",
 * profiles can be set with the --profile option in the command line (if an
 * appropriate launcher is used), or setting the enRoute.configurer.profile
 * System property is set.
 * <p>
 * Macros are fully supported. The variable order is configuration, system
 * properties, settings in ~/.enRoute/settings.json.
 * <p>
 * The configurer can also refer to binary resources with @{resource-path}. This
 * pattern requires a resource path inside the bundle. This resource is copied
 * to the local file system and the macro is replaced with the corresponding
 * path.
 * <p>
 * The configurer reads
 */

@ProvideCapability(ns = ExtenderNamespace.EXTENDER_NAMESPACE, name = ConfigurerConstants.CONFIGURER_EXTENDER_NAME, version = ConfigurerConstants.CONFIGURER_EXTENDER_VERSION)
@Component(service = {
		ConfigurationDone.class, Object.class
}, immediate = true)
public class Configurer implements ConfigurationDone {
	private static final String					FORCE				= "force";
	private static final String					CONFIGURER_PRECIOUS	= ":configurer:precious";
	private static final String					CONFIGURER_POLICY	= ":configurer:policy";
	private static final String					LOGICAL_PID_KEY		= "._osgi.enroute.pid";
	private static final String					BUNDLE_KEY			= "._osgi.enroute.bundle";
	private static final String					EN_ROUTE_PROFILE	= "enRoute.profile";
	private static final String					CONFIGURER_EXTRA	= "enRoute.configurer.extra";
	private static final String					CONFIGURATION_LOC	= "configuration/configuration.json";

	public static final JSONCodec				codec				= new JSONCodec();
	public static final Converter				converter			= new Converter();
	static Pattern								PROFILE_PATTERN		= Pattern.compile("\\[([a-zA-Z0-9]+)\\](.*)");
	Settings									settings			= new Settings("~/.enRoute/settings.json");
	LogService									log;
	BundleTracker<?>							tracker;
	ConfigurationAdmin							cm;
	String										profile;
	File										dir;
	Map<String, String>							base;
	Bundle										currentBundle;
	private final AtomicReference<Coordinator>	coordinatorRef		= new AtomicReference<>();

	/*
	 * Track all bundles and read their configuration.
	 */
	@Activate
	void activate(BundleContext context) throws Exception {
		//
		// Reserve space for the resources
		//
		dir = context.getDataFile("resources");
		dir.mkdirs();

		Map<String, String> map = new HashMap<>();
		map.putAll(settings);
		map.putAll(converter.convert(new TypeReference<Map<String, String>>() {
		}, System.getProperties()));
		this.base = Collections.unmodifiableMap(map);

		Coordinator coordinator = coordinatorRef.get();
		Coordination coordination = coordinator != null
				? coordinator.begin("enRoute.configurer", TimeUnit.SECONDS.toMillis(20))
				: null;
		try {
			tracker = new BundleTracker<Object>(context, Bundle.ACTIVE | Bundle.STARTING, null) {

				@Override
				public Object addingBundle(Bundle bundle, BundleEvent event) {
					try {

						boolean defined = true;
						String h = bundle.getHeaders().get(ConfigurationDone.BUNDLE_CONFIGURATION);
						if (h == null) {
							//
							// We use a default configuration if the header is
							// not set for convenience
							//
							h = CONFIGURATION_LOC;
							defined = false;
						}

						h = h.trim();
						if (h.isEmpty())
							//
							// If there is an empty value, we assume
							// the user does not want it ...
							//
							return null;

						log.log(LogService.LOG_INFO, "Reading configurations for bundle " + bundle.getSymbolicName()
								+ " " + bundle.getVersion() + " in " + h);
						for (String entry : h.split(",")) {
							URL url = bundle.getEntry(entry);
							log.log(LogService.LOG_INFO, "Reading configuration for bundle " + bundle.getSymbolicName()
									+ " " + bundle.getVersion() + " in " + entry + " " + url);

							if (url == null) {
								return null;
							}

							String s = IO.collect(url);
							if (s == null) {
								if (defined)
									log.log(LogService.LOG_INFO,
											"Cannot find configuration for bundle " + bundle.getSymbolicName() + " "
													+ bundle.getVersion() + " in " + h + " " + url);
								return null;
							}

							configure(bundle, s);
						}
					} catch (IOException e) {
						log.log(LogService.LOG_ERROR, "Failed to set configuration for " + bundle, e);
					}

					//
					// We do not have to track this, we leave the configuration
					// in cm. TODO purge command
					//
					return null;
				}
			};

			tracker.open(); // this will iterate over all bundles synchronously

			//
			// Check if we have any extra configuration via system properties
			//

			String s = System.getProperty(CONFIGURER_EXTRA);
			if (s != null)
				configure(context.getBundle(), s);

		} catch (Exception e) {
			if (coordination != null)
				coordination.fail(e);
		} finally {
			if (coordination != null)
				coordination.end();
		}

	}

	/*
	 * Deactivate
	 */
	void deactivate() {
		tracker.close();
	}

	/*
	 * Main work horse
	 */
	static Pattern	AT_MACRO_P	= Pattern.compile("@\\{(?>.*\\})");
	static Pattern	MACRO_P		= Pattern.compile("(?!\\\\)$\\{(?>.*\\})");

	void configure(Bundle bundle, String data) {
		try {

			//
			// First replace @{ ... } with ${...}. This is
			// optional but allows easy separation from other
			// macro processors
			//

			data = data.replaceAll("(?!\\\\)@\\{", "\\${");

			ReplacerAdapter replacer = new ReplacerAdapter(base);
			replacer.target(this);

			currentBundle = bundle;
			data = replacer.process(data);
			currentBundle = null;

			if (profile == null)
				profile = base.containsKey(EN_ROUTE_PROFILE) ? base.get(EN_ROUTE_PROFILE) : "debug";

			log.log(LogService.LOG_INFO, "Profile is " + profile);

			//
			// Since we need to work with Dictionary, it is convenient to
			// get the result in a list of Hashtable (which implements
			// Dictionary).
			//

			final List<Hashtable<String, Object>> list = codec.dec().from(data)
					.get(new TypeReference<List<Hashtable<String, Object>>>() {
					});

			//
			// Process each dictionary
			//

			for (Map<String, Object> d : list) {

				Hashtable<String, Object> newer = new Hashtable<String, Object>();

				getDict(d, newer);

				//
				// We now have a dictionary and should update config admin.
				// The
				// dictionary
				// must contain a pid, and may contain a factory pid.
				// A factory pid implies that the pid is symbolic since it
				// will
				// be assigned
				// by config admin.
				//

				String factory = (String) newer.get("service.factoryPid");
				String pid = (String) newer.get("service.pid");

				if (pid == null) {
					log.log(LogService.LOG_ERROR, "Invalid configuration, no PID specified: " + newer);
					continue;
				}

				Configuration configuration;

				if (factory != null) {

					//
					// We have a factory configuration, so the PID is
					// symbolic
					// now
					//

					newer.put(LOGICAL_PID_KEY, pid);
					newer.put(BUNDLE_KEY, bundle.getBundleId());

					//
					// We use the symbolic PID to find an existing record.
					// if it does not exist, we create a new one.
					//

					Configuration instances[] = cm.listConfigurations("(&(" + LOGICAL_PID_KEY + "=" + pid
							+ ")(service.factoryPid=" + factory + "))");
					if (instances == null || instances.length == 0) {
						configuration = cm.createFactoryConfiguration(factory, "?");
					} else {
						configuration = instances[0];
					}

				} else {

					//
					// normal target configuration
					//

					configuration = cm.getConfiguration(pid, "?");
				}

				Dictionary<?, ?> current = configuration.getProperties();
				if (current != null) {

					Object policy = newer.remove(CONFIGURER_POLICY);
					if (!FORCE.equals(policy))
						continue;

					preservePreciousFields(bundle, newer, current);

					if (isEqual(newer, current))
						continue;

				}

				configuration.setBundleLocation("?");
				configuration.update(newer);
			}
		} catch (Exception e) {
			log.log(LogService.LOG_ERROR, "While configuring " + bundle.getBundleId() + ", configuration is " + data,
					e);
		}
	}

	private void preservePreciousFields(Bundle bundle, Hashtable<String, Object> newer, Dictionary<?, ?> current) {
		Object precious = newer.remove(CONFIGURER_PRECIOUS);
		if (precious instanceof Collection) {
			for (Object field : ((Collection<?>) precious)) {
				if (field instanceof String) {
					Object value = current.get(field);
					if (value != null) {
						newer.put((String) field, value);
					}
				} else {
					log.log(LogService.LOG_INFO, CONFIGURER_PRECIOUS + " field names must be a string, found: "
							+ field.getClass() + " in bundle " + bundle);
				}
			}
		} else if (precious != null) {
			log.log(LogService.LOG_INFO, CONFIGURER_PRECIOUS + " must be a collection, it is a " + precious.getClass()
					+ " in bundle " + bundle);
		}
	}

	/*
	 * Build a dictionary from the JSON map we got. We handle the profiles here
	 * by looking at the key, if it is a profile key, and the profile matches,
	 * we add the non-profile key. Otherwise we do some other stuff like logging
	 * and comments.
	 */
	private void getDict(Map<String, Object> d, Hashtable<String, Object> dictionary) throws Exception {
		for (Entry<String, Object> e : d.entrySet()) {

			String key = e.getKey();
			Object value = e.getValue();

			Matcher m = PROFILE_PATTERN.matcher(e.getKey());
			boolean prfile = false;

			if (m.matches()) {

				//
				// Check if a key is a profile key (starts wit [...]
				// if so, fix it up. That is, if it matches out
				// current profile, we remove the prefix and use
				// it, otherwise we ignore it
				//

				String profile = m.group(1);
				if (!profile.equals(this.profile))
					continue;

				key = m.group(2);
				prfile = true;

			} else if (e.getKey().equals(".log")) {
				//
				// .log entries are ignored but send to the logger
				//
				log.log(LogService.LOG_INFO, converter.convert(String.class, d.get(".log")));
				continue;

			} else if (e.getKey().startsWith(".comment"))
				//
				// Keys tha start with .comment are ignored
				//
				continue;

			//
			// Check if all macros were resolved
			//
			if (value != null && value instanceof String) {
				Matcher matcher = MACRO_P.matcher((String) value);
				if (matcher.find()) {
					log.log(LogService.LOG_ERROR, "Configuration has detected macros that are not resolved: " + key
							+ "=" + value);
				}
			}

			//
			// Profile keys should always override, Otherwise, first one wins
			//
			if (prfile || !dictionary.containsKey(key))
				dictionary.put(key, value);

		}
	}

	/*
	 * We do not want to update a configuration unless it really has been
	 * changed
	 */

	@SuppressWarnings("unchecked")
	private boolean isEqual(Hashtable<String, Object> a, Dictionary<?, ?> b) {

		for (Entry<String, Object> e : a.entrySet()) {
			if (e.getKey().equals("service.pid"))
				continue;

			Object value = b.get(e.getKey());
			if (value == e.getValue())
				continue;

			if (value == null)
				return false;

			if (e.getValue() == null)
				return false;

			if (value.equals(e.getValue()))
				continue;

			if (value.getClass().isArray()) {
				Object[] aa = {
						value
				};
				Object[] bb = {
						e.getValue()
				};
				if (!Arrays.deepEquals(aa, bb))
					return false;
			} else if (value instanceof Collection && e.getValue() instanceof Collection) {
				ExtList<Object> aa = new ExtList<Object>((Collection<Object>) value);
				ExtList<Object> bb = new ExtList<Object>((Collection<Object>) e.getValue());
				if (!aa.equals(bb))
					return false;
			} else {
				log.log(LogService.LOG_INFO,
						"Updating config because " + a.get("service.pid") + " has a different value for " + e.getKey()
								+ ". Old value " + value + ", new value: " + e.getValue());
				return false;
			}
		}
		return true;
	}

	/*
	 * This macro gets a resource from the bundle. It will copy this resource
	 * somewhere on the file system. It will return the actual file name. This
	 * is very useful for certificates
	 */
	public String _resource(String args[]) throws IOException {
		if (args.length != 2) {
			log.log(LogService.LOG_ERROR, "The ${resource} macro only takes 1 argument, the resource path");
			return null;
		}

		URL url = currentBundle.getEntry(args[1]);
		if (url == null) {
			log.log(LogService.LOG_ERROR, "The ${resource;" + args[1] + "} macro cannot find the bundle resource ");
			return null;
		}

		String path = url.getPath();

		//
		// Make sure that the path is safe so someone cannot use this
		// to access the root or other files outside its data area
		//

		if (path.startsWith("/") || path.startsWith("~"))
			path = path.substring(1);

		String safe = path.replaceAll("[^\\w\\d._-]|\\.\\.", "_");
		File dir = currentBundle.getDataFile("");
		File out = IO.getFile(dir, safe);

		out.getParentFile().mkdirs();
		if (!out.getParentFile().isDirectory()) {
			log.log(LogService.LOG_ERROR, "Cannot create configuration directory " + dir + " in bundle "
					+ currentBundle);
		}

		try {
			IO.copy(url.openStream(), out);
		} catch (Exception e) {
			log.log(LogService.LOG_ERROR, "Cannot copy a resource " + out + " from bundle " + currentBundle
					+ " resource " + url);
		}

		String s= out.getAbsolutePath();
		
		// since the value is used in a JSON file
		// we must escape it
		
		return escapeToJsonString(s);
	}

	/*
	 * Macro to provide current bundle id
	 */

	public String _bundleid(String args[]) {
		if (args.length != 1) {
			log.log(LogService.LOG_ERROR, "The ${bundleid} macro takes no parameters");
			return null;
		}

		return currentBundle.getBundleId() + "";
	}

	/*
	 * Macro to provide current location
	 */

	public String _location(String args[]) {
		if (args.length != 1) {
			log.log(LogService.LOG_ERROR, "The ${location} macro takes no parameters");
			return null;
		}

		return escapeToJsonString(currentBundle.getLocation());
	}

	private String escapeToJsonString(String s) {
		StringBuilder app = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"' :
					app.append("\\\"");
					break;

				case '\\' :
					app.append("\\\\");
					break;

				case '\b' :
					app.append("\\b");
					break;

				case '\f' :
					app.append("\\f");
					break;

				case '\n' :
					app.append("\\n");
					break;

				case '\r' :
					app.append("\\r");
					break;

				case '\t' :
					app.append("\\t");
					break;

				default :
					if (Character.isISOControl(c)) {
						app.append("\\u");
						app.append("0123456789ABCDEF".charAt(0xF & (c >> 12)));
						app.append("0123456789ABCDEF".charAt(0xF & (c >> 8)));
						app.append("0123456789ABCDEF".charAt(0xF & (c >> 4)));
						app.append("0123456789ABCDEF".charAt(0xF & (c >> 0)));
					} else {
						app.append(c);
					}
			}
		}
		if ( app.length() == s.length())
			return s;
		else
			return app.toString();
	}

	@Reference
	void setLogService(LogService log) {
		this.log = log;
	}

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	void setCoordinator(Coordinator coordinator) {
		coordinatorRef.set(coordinator);
	}

	void unsetCoordinator(Coordinator coordinator) {
		coordinatorRef.compareAndSet(coordinator, null);
	}

	@Reference
	void setCM(ConfigurationAdmin cm) {
		this.cm = cm;
	}

	/*
	 * We need the launcher's arguments to get a profile
	 */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, target = "(launcher.arguments=*)")
	synchronized void setLauncher(Object obj, Map<String, Object> props) {
		String[] args = (String[]) props.get("launcher.arguments");
		for (int i = 0; i < args.length - 1; i++) {
			if (args[i].equals("--profile")) {
				this.profile = args[i+1];
				return;
			}
		}
	}

	void unsetLauncher(Object obj) {

	}
}
