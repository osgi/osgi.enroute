package osgi.enroute.web.server.provider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.bnd.osgi.Verifier;
import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;
import aQute.lib.io.IO;
import aQute.libg.glob.Glob;
import osgi.enroute.http.capabilities.RequireHttpImplementation;
import osgi.enroute.web.server.cache.Cache;
import osgi.enroute.web.server.cache.CacheFile;
import osgi.enroute.web.server.cache.CacheFileFactory;
import osgi.enroute.web.server.config.WebServerConfig;
import osgi.enroute.web.server.exceptions.ExceptionHandler;
import osgi.enroute.web.server.exceptions.NotFound404Exception;
import osgi.enroute.webserver.capabilities.WebServerConstants;

/**
 * This class adds support for Web Resources. A Web Resource is a resource
 * delivered from a bundle that is controlled through Requirements and
 * Capabilities. It enables the use of web resources without having to know the
 * actual location of the web resource in the system. The path to the resource
 * can even be private. An application can refer to the web resources it needs
 * by creating a requirement to a webresource capability. The requirement has a
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
 * 			&quot;bootstrap.css&quot;
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
 *       <link href="/osgi.enroute.webresource/bundle/1.2.3/*.css" type=
"text/css" rel="stylesheet">
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
 * 'https://github.com/osgi/design/blob/master/rfps/rfp-0171-Web-Resources.pdf?raw=true'
 * > RFP 171 Web Resources (PDF)</a>
 */
@ProvideCapability(ns = ExtenderNamespace.EXTENDER_NAMESPACE, name = WebServerConstants.WEB_SERVER_EXTENDER_NAME, version = WebServerConstants.WEB_SERVER_EXTENDER_VERSION)
@RequireHttpImplementation
@Component(property = {
		HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/" + WebresourceServlet.OSGI_ENROUTE_WEBRESOURCE
				+ "/*",
		Constants.SERVICE_RANKING + ":Integer=101", "addTrailingSlash=true"
}, service = Servlet.class, immediate = true, name = WebresourceServlet.NAME, configurationPid = BundleMixinServer.NAME, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class WebresourceServlet extends HttpServlet {

	private static final long	serialVersionUID			= 1L;

	static final String			NAME						= "osgi.enroute.simple.webresource";
	public static final String	OSGI_ENROUTE_WEBRESOURCE	= "osgi.enroute.webresource";

	final static Pattern		WEBRESOURCES_P				= Pattern.compile("osgi.enroute.webresource/(?<bsn>"
			+ Verifier.SYMBOLICNAME_STRING + "+)/(?<version>" + Verifier.VERSION_STRING + ")/(?<glob>.+)");

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

	final TypeReference<List<String>>	listOfStrings	= new TypeReference<List<String>>() {};
	WebServerConfig						config;
	private Cache						cache;
	private ResponseWriter				writer;
	private ExceptionHandler			exceptionHandler;
	private LogService					log;
	boolean								proxy;

	@Activate
	void activate(WebServerConfig config, BundleContext context) throws Exception {
		this.config = config;
		this.writer = new ResponseWriter(config);
		this.exceptionHandler = new ExceptionHandler(config.addTrailingSlash(), log);
		proxy = !config.noproxy();
	}

	@Override
	protected void doGet(HttpServletRequest rq, HttpServletResponse rsp) throws ServletException, IOException {
		try {
			String path = rq.getRequestURI();

			if (path == null)
				throw new NotFound404Exception(null);

			if (path.startsWith("/"))
				path = path.substring(1);

			// Useless check??
			if (!path.startsWith(OSGI_ENROUTE_WEBRESOURCE))
				throw new NotFound404Exception(null);

			CacheFile c;
			cache.lock();
			try {
				c = cache.get(path);
				if (c == null || c.isExpired()) {
					if (proxy && path.startsWith("$"))
						// Not sure what this does...
						c = cache.findCacheFileByPath(path);
					else
						c = find(path);

					if (c == null) {
						throw new NotFound404Exception(null);
					} else
						cache.put(path, c);
				}
			}
			finally {
				cache.unlock();
			}

			if (c == null || !c.isSynched())
				throw new NotFound404Exception(null);

			writer.writeResponse(rq, rsp, c);
		}
		catch (Exception e) {
			e.printStackTrace();
			exceptionHandler.handle(rq, rsp, e);
		}
	}

	/*
	 * The core find method. If the stuff matches, then we create a file and
	 * return it in a cache object. The file is stored in the bundle's directory
	 * so it gets cleaned up when the bundle is uninstalled.
	 */
	CacheFile find(String path) throws Exception {

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

					for (String resourceWithCommas : resources) {

						// Felix does not split comma separated List attributes
						// as Equinox does. So we do it for them.
						
						for (String resource : resourceWithCommas.split("\\s*(?!\\\\),\\s*")) {
							if (glob.matcher(resource).matches()) {
								URL url = provider.getBundle().getEntry(root + resource);
								if (url != null) {
									webresources.add(new WR(url, priority, order++));
								} else {
									log.log(LogService.LOG_ERROR, "A web resource " + resource + " from " + requirement
											+ " in bundle " + bsn + "-" + version);
									return null;
								}
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
			webresources.stream().sorted().map((wr) -> wr.resource).distinct().forEach((url) ->
			{
				try {
					IO.copy(url.openStream(), pout);
					pout.println("\n");
				}
				catch (Exception e) {
					log.log(LogService.LOG_ERROR, "A web resource fails " + url + " in bundle " + bsn + "-" + version,
							e);
				}
			});
		}

		//
		// We could do this work multiple times so we need to make the rename
		// atomic. The duplication is a bit of waste but should be harmless.
		//

		tmp.renameTo(file);

		return CacheFileFactory.newCacheFile(file, b, 0, file.getAbsolutePath());
	}

	/**
	 * make sure the name does not contain any offending characters
	 */

	static Pattern BADCHAR_P = Pattern.compile("[^a-zA-Z-_.$@%+]");

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
		BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
		for (Bundle b : context.getBundles()) {
			if (bsn.equals(b.getSymbolicName()) && v.equals(b.getVersion()))
				return b;
		}
		return null;
	}

	@Deactivate
	void deactivate() {}

	@Reference
	void setLog(LogService log) {
		this.log = log;
	}

	@Reference
	void setCache(Cache cache) {
		this.cache = cache;
	}
}