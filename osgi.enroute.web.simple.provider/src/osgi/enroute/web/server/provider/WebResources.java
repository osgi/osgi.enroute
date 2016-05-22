package osgi.enroute.web.server.provider;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import org.osgi.framework.*;
import org.osgi.framework.wiring.*;
import org.slf4j.*;

import aQute.bnd.osgi.*;
import aQute.lib.converter.*;
import aQute.lib.io.*;
import aQute.libg.glob.*;
import osgi.enroute.web.server.cache.*;

/**
 * This class adds support for web resources. A Web Resource is a resource
 * delivered from a bundle that is controlled through Requirements and
 * Capabilities. It enables using web resources without having to know the
 * actual path that the web resource has in the system, actually, the path can
 * be private. An application can refer to its web resources by creating a
 * requirement to a webresource capability. The requirement has a
 * {@code resource} and {@code priority} property. If the application now refers
 * to a URI {@value #OSGI_ENROUTE_WEBRESOURCE}{@code /<bundle>/<version>/<type>}
 * this code will append all the required resources in order of occurrence and
 * priority. The {@code type} is a Glob expression that must match the file path
 * of the URL. Additionally, the content of the {@code web} resource directory
 * is traversed recursively for any resources that match the glob expression.
 * <p>
 * Example using manifest headers:
 * 
 * <pre>
 * 
 * &#064;RequireCapability(ns = &quot;osgi.enroute.webresource&quot;, filter = &quot;(osgi.enroute.webresource=/google/angular)&quot;)
 * public @interface AngularWebResource {
 * 	String[] resource();
 * 
 * 	int priority() default 1000;
 * }
 * 
 * &#064;RequireCapability(ns = &quot;osgi.enroute.webresource&quot;, filter = &quot;(osgi.enroute.webresource=/twitter/bootstrap)&quot;)
 * public @interface BootstrapWebResource {
 * 	String[] resource() default {
 * 		&quot;bootstrap.css&quot;
 * 	};
 * 
 * 	int priority() default 1000;
 * }
 * 
 * &#064;BootstrapWebResource(resource = {
 * 		&quot;angular.js&quot;, &quot;angular-resource.js&quot;
 * })
 * &#064;AngularWebResource(resource = {
 * 		&quot;angular.js&quot;, &quot;angular-resource.js&quot;
 * })
 * public class App {
 * 
 * }
 * </pre>
 * 
 * <pre>
 * {@code
 * index.html
 *   <html>
 *     <head>
 *       <link href="/osgi.enroute.webresource/bundle/1.2.3/*.css" type="text/css" rel="stylesheet">
 *     </head>
 *     <body>
 *       ...
 *       
 *       <script src="/osgi.enroute.webresource/bundle/1.2.3/*.js"></script>
 *     </body>
 *   </html>
 *     
 * }
 * </pre>
 * <p>
 * <a href=
 * 'https://github.com/osgi/design/blob/master/rfps/rfp-0171-Web-Resources.pdf?r
 * a w = t r u e ' > RFP 171 Web Resources (PDF)</a>
 */
public class WebResources {
	private static final String			OSGI_ENROUTE_WEBRESOURCE	= "osgi.enroute.webresource";

	final static Pattern				WEBRESOURCES_P				= Pattern
																			.compile("osgi.enroute.webresource/(?<bsn>"
																					+ Verifier.SYMBOLICNAME_STRING
																					+ "+)/(?<version>"
																					+ Verifier.VERSION_STRING
																					+ ")/(?<glob>.+)");

	final TypeReference<List<String>>	listOfStrings				= new TypeReference<List<String>>() {};
	static Logger						logger						= LoggerFactory.getLogger(WebResources.class);
	final BundleContext					context;
	final WebServer						ws;
	final CacheFactory					cacheFactory;

	/**
	 * Constructor
	 * 
	 * @param ws
	 *            the web server to create cache objects
	 * @param context
	 *            To see the bundles
	 */
	WebResources(WebServer ws, CacheFactory cacheFactory, BundleContext context) {
		this.context = context;
		this.cacheFactory = cacheFactory;
		this.ws = ws;
	}

	/*
	 * Helper class to sort the entries according to their priority and order.
	 */
	static class WR implements Comparable<WR> {
		URL	resource;
		int	priority;
		int	order;

		public WR(URL url, int priority, int order) {
			this.resource = url;
			this.priority = priority;
			this.order = order;
		}

		@Override
		public int compareTo(WR o) {
			int result = Integer.compare(o.priority, priority);
			if (result != 0)
				return result;

			return Integer.compare(order, o.order);
		}
	}

	/*
	 * The core find method. If the stuff matches, then we create a file and
	 * return it in a cache object. The file is stored in the bundle's directory
	 * so it gets cleaned up when the bundle is uninstalled.
	 */
	Cache find(String path) throws Exception {

		//
		// Verify if it actually is for us in the fastest way possible
		//

		if (!path.startsWith(OSGI_ENROUTE_WEBRESOURCE))
			return null;

		//
		// Parse the path so we get the bundle, version, and glob
		//

		Matcher matcher = WEBRESOURCES_P.matcher(path);
		if (!matcher.matches())
			return null;

		String bsn = matcher.group("bsn");
		String version = matcher.group("version");

		//
		// Check if there is such a bundle.
		//

		Bundle b = getBundle(bsn, version);
		if (b == null)
			return null;

		//
		// Check if we have any wiring
		//

		BundleWiring wiring = b.adapt(BundleWiring.class);
		List<BundleWire> wires = wiring.getRequiredWires(OSGI_ENROUTE_WEBRESOURCE);
		if (wires.isEmpty())
			return null;

		List<WR> webresources = new ArrayList<>();
		int order = 1000;
		String globss = matcher.group("glob");
		boolean literal = globss.indexOf('*') < 0 && globss.indexOf('?') < 0;
		Glob glob = new Glob(globss);

		//
		// traverse the wiring and process the
		// requirements. The requirements provide the
		// resource name plus extension that must match the
		// glob expr. The capabilities provide the actual
		// path in the bundle
		//

		for (BundleWire wire : wires) {

			BundleRequirement requirement = wire.getRequirement();
			BundleCapability capability = wire.getCapability();
			BundleRevision provider = capability.getResource();

			Map<String,Object> attrs = requirement.getAttributes();

			//
			// Get the root path
			//

			String root = (String) capability.getAttributes().get("root");
			if (root == null)
				root = "";

			if (!root.isEmpty() && !root.endsWith("/"))
				root = root + "/";

			if (literal) {
				URL url = provider.getBundle().getEntry(root + globss);
				if (url != null)
					webresources.add(new WR(url, 0, order++));
			} else {

				//
				// We allow single entry or multiple entries for resources
				// so we use the converter
				//

				int priority = (Integer) Converter.cnv(Integer.class, attrs.get("priority"));
				List<String> resources = Converter.cnv(listOfStrings, attrs.get("resource"));
				if (resources != null) {

					//
					// Add all the resources to the list
					//

					for (String resource : resources) {
						if (glob.matcher(resource).matches()) {
							URL url = provider.getBundle().getEntry(root + resource);
							if (url != null) {
								webresources.add(new WR(url, priority, order++));
							} else {
								logger.error("A web resource " + resource + " from " + requirement + " in bundle "
										+ bsn + "-" + version);
								return null;
							}
						}
					}
				} else {
					//
					// If no resources are specified we fill it with
					// the existing resources
					//

					List<URL> entries = Collections.list(provider.getBundle().findEntries(root, "*", true));

					for (URL url : entries) {
						String p = url.getPath();
						int n = p.lastIndexOf('/');
						if (n >= 0)
							p = p.substring(n + 1);

						if (glob.matcher(p).matches()) {
							webresources.add(new WR(url, priority, order++));
						}
					}
				}
			}
		}

		//
		// Add the local resources by traversing the {@code /web} directory
		//

		Enumeration<URL> entries = b.findEntries("web/", "*", true);
		if (entries != null) {
			while (entries.hasMoreElements()) {
				URL url = entries.nextElement();
				String rpath = url.getPath().substring(4);
				if (glob.matcher(rpath).matches())
					webresources.add(new WR(url, -1, order++));
			}
		}

		//
		// Write a cache file in the directory of the bundle
		//
		String validFileName = toValidFileName(glob.toString());
		File file = b.getDataFile(OSGI_ENROUTE_WEBRESOURCE + "/" + version + "/" + validFileName);
		file.getParentFile().mkdirs();
		File tmp = new File(file.getParentFile(), validFileName + "-tmp");

		//
		// Collect the resources in the proper order and duplicates removed.
		//

		try (FileOutputStream out = new FileOutputStream(tmp); PrintStream pout = new PrintStream(out);) {
			webresources.stream().sorted().map((wr) -> wr.resource).distinct().forEach((url) -> {
				try {
					IO.copy(url.openStream(), pout);
					pout.println("\n");
				}
				catch (Exception e) {
					logger.error("A web resource fails " + url + " in bundle " + bsn + "-" + version, e);
				}
			});
		}

		//
		// We could do this work multiple times so we need to make the rename
		// atomic. The duplication is a bit of waste but should be harmless.
		//

		tmp.renameTo(file);

		return cacheFactory.newCache(file, b, file.getAbsolutePath());
	}

	/**
	 * make sure the name does not contain any offending characters
	 */

	static Pattern	BADCHAR_P	= Pattern.compile("[^a-zA-Z-_.$@%+]");

	static String toValidFileName(String string) throws UnsupportedEncodingException {
		StringBuffer sb = new StringBuffer();
		Matcher m = BADCHAR_P.matcher(string);
		while (m.find()) {
			char x = m.group(0).charAt(0);
			if (x >= 128 || x <= 0)
				m.appendReplacement(sb, "");
			else if (x <= 15)
				m.appendReplacement(sb, "%0" + Integer.toHexString(x));
			else
				m.appendReplacement(sb, "%" + Integer.toHexString(x));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/*
	 * Helper to find a bundle
	 */
	private Bundle getBundle(String bsn, String version) {
		Version v = new Version(version);
		for (Bundle b : context.getBundles()) {
			if (bsn.equals(b.getSymbolicName()) && v.equals(b.getVersion()))
				return b;
		}
		return null;
	}
}
