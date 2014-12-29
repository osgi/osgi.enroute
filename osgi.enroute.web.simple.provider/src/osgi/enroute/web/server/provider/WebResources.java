package osgi.enroute.web.server.provider;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import org.osgi.framework.*;
import org.osgi.framework.wiring.*;
import org.slf4j.*;

import osgi.enroute.web.server.provider.WebServer.Cache;
import aQute.bnd.osgi.*;
import aQute.lib.converter.*;
import aQute.lib.io.*;
import aQute.libg.glob.*;

public class WebResources {
	private static final String			OSGI_ENROUTE_WEBRESOURCE	= "osgi.enroute.webresource";

	final static Pattern				WEBRESOURCES_P				= Pattern
																			.compile("osgi.enroute.webresource/(?<bsn>"
																					+ Verifier.SYMBOLICNAME_STRING
																					+ "+)/(?<version>"
																					+ Verifier.VERSION_STRING
																					+ ")/(?<glob>\\*\\..+)");

	final TypeReference<List<String>>	listOfStrings				= new TypeReference<List<String>>() {};
	static Logger						logger						= LoggerFactory.getLogger(WebResources.class);
	final BundleContext					context;
	final WebServer						ws;

	WebResources(WebServer ws, BundleContext context) {
		this.context = context;
		this.ws = ws;
	}

	static class WR implements Comparable<WR> {
		URL	resource;
		int	priority;

		public WR(URL url, int priority) {
			resource = url;
			this.priority = priority;
		}

		@Override
		public int compareTo(WR o) {
			if (priority > o.priority)
				return 1;

			if (priority < o.priority)
				return -1;

			return resource.getFile().compareTo(o.resource.getFile());
		}
	}

	Cache find(String path) throws Exception {
		if (!path.startsWith(OSGI_ENROUTE_WEBRESOURCE))
			return null;

		Matcher matcher = WEBRESOURCES_P.matcher(path);
		if (!matcher.matches())
			return null;

		String bsn = matcher.group("bsn");
		String version = matcher.group("version");
		Glob glob = new Glob(matcher.group("glob"));

		Bundle b = getBundle(bsn, version);
		if (b == null)
			return null;

		BundleWiring wiring = b.adapt(BundleWiring.class);
		List<BundleWire> wires = wiring.getRequiredWires(OSGI_ENROUTE_WEBRESOURCE);
		if (wires.isEmpty())
			return null;

		List<WR> webresources = new ArrayList<>();

		for (BundleWire wire : wires) {
			BundleRequirement requirement = wire.getRequirement();
			BundleCapability capability = wire.getCapability();
			BundleRevision provider = capability.getResource();

			Map<String,Object> attrs = requirement.getAttributes();
			List<String> resources = Converter.cnv(listOfStrings, attrs.get("resource"));
			int priority = Converter.cnv(Integer.class,attrs.get("priority"));

			for (String resource : resources) {
				if (glob.matcher(resource).matches()) {
					URL url = provider.getBundle().getEntry(resource);
					if (url != null) {
						webresources.add(new WR(url, priority));
					} else {
						logger.error("A web resource " + resource + " from " + requirement + " in bundle " + bsn + "-"
								+ version);
						return null;
					}
				}
			}
		}

		File file = b.getDataFile(OSGI_ENROUTE_WEBRESOURCE + "/" + glob.toString());
		File tmp = new File(file.getParentFile(), glob.toString());

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

		tmp.renameTo(file);

		return ws.new Cache(file);
	}

	private Bundle getBundle(String bsn, String version) {
		Version v = new Version(version);
		for (Bundle b : context.getBundles()) {
			if (bsn.equals(b.getSymbolicName()) && v.equals(b.getVersion()))
				return b;
		}
		return null;
	}
}
