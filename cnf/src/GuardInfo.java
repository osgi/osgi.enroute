import java.io.ByteArrayOutputStream;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import osgi.enroute.guard.dto.Guard;
import osgi.enroute.guard.dto.Guard.Requirement;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.json.JSONCodec;

/**
 * Create a package version list so the guard can verify that version match up.
 * We create two different lists here:
 * <ul>
 * <li>
 */
public class GuardInfo implements AnalyzerPlugin {
	private static final String PATH = "enroute/sine-qua-non.json";
	final static String COMMAND = "-guardinfo";
	final static Pattern CAPABILITY_P = Pattern
			.compile("\\s*(?<ns>[-_.\\w\\d]+)\\s*:\\s*(?<name>[-_.\\w\\d]+)\\s*=\\s*(?<pack>[-_.\\w\\d]+)\\s*");

	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		try {
			String cmd = analyzer.getProperty(COMMAND);
			Parameters p = new Parameters(cmd);
			if (p.isEmpty())
				return false;

			Guard guard = new Guard();

			analyzer.trace("In guard info %s", p);

			for (Entry<String, Attrs> e : p.entrySet()) {
				analyzer.trace("Entry %s = %s", e.getKey(), e.getValue());
				String key = Processor.removeDuplicateMarker(e.getKey());
				switch (key) {
				case "services":
					doServices(analyzer, e.getValue(), guard);
					break;

				case "packages":
					doPackages(analyzer, e.getValue(), guard);
					break;

				case "capabilities":
					doCapabilities(analyzer, e.getValue(), guard);
					break;

				default:
					analyzer.error("Unknown type for GuardInfo %s", e.getKey());
					break;
				}
			}
			System.out.println("Guard" + guard);

			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			new JSONCodec().enc().to(bout).put(guard);
			EmbeddedResource resource = new EmbeddedResource(
					bout.toByteArray(), 0);
			analyzer.getJar().putResource(PATH, resource);
			analyzer.getJar().getResource(PATH).write(System.out);
			return false;
		} catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
	}

	/*
	 * Create a list of all packages listed in the set attribute. For each
	 * package the value is the default semantic consumer import range.
	 */
	private void doPackages(Analyzer analyzer, Attrs attrs, Guard guard) {
		String set = attrs.get("set");
		if (set != null && !set.isEmpty()) {
			String[] members = set.trim().split("\\s*,\\s*");
			for (String member : members) {
				PackageRef pref = analyzer.getPackageRef(member);

				if (pref.isJava() || pref.isMetaData())
					continue;

				Attrs pattrs = getAttrs(analyzer, member);

				if (pattrs == null)
					analyzer.error("GuardInfo: cannot find package %s", member);
				else {
					String version = pattrs.getVersion();
					if (version == null)
						analyzer.error(
								"GuardInfo: cannot find version for package %s",
								member);
					else {
						if (!Verifier.isVersion(version))
							analyzer.error(
									"GuardInfo: invalid version %s for package %s",
									version, member);
						Version low = new Version(version)
								.getWithoutQualifier();
						Version high = new Version(low.getMajor(),
								low.getMinor() + 1, 0);

						VersionRange range = new VersionRange("[" + low + ","
								+ high + ")");
						Requirement r = new Requirement();
						r.ns = "osgi.wiring.package";
						r.description = "Import-Package " + pref + "version="
								+ range;
						r.filter = "(&(osgi.wiring.package=" + pref + ")"
								+ range.toFilter() + ")";
						guard.requirements.add(r);
					}
				}
			}
		}

	}

	private void doCapabilities(Analyzer analyzer, Attrs attrs, Guard guard) {
		String set = attrs.get("set");
		if (set != null && !set.isEmpty()) {
			String[] members = set.trim().split("\\s*,\\s*");
			for (String member : members) {
				Matcher m = CAPABILITY_P.matcher(member);
				if (!m.matches()) {
					analyzer.error("Invalid capability definition %s", member);
				}
				String ns = m.group("ns");
				String name = m.group("name");
				String pack = m.group("pack");

				Attrs pattrs = getAttrs(analyzer, pack);

				if (pattrs == null)
					analyzer.error(
							"GuardInfo: cannot find package %s for capability %s:%s",
							pack, ns, name);
				else {
					String version = pattrs.getVersion();
					if (version == null)
						analyzer.error(
								"GuardInfo: cannot find version from package %s for capability %s:%s",
								pack, ns, name);
					else {
						if (!Verifier.isVersion(version))
							analyzer.error(
									"GuardInfo: invalid version %s for capability %s = %s",
									version, member, pack);
						Version low = new Version(version)
								.getWithoutQualifier();
						Version high = new Version(low.getMajor(),
								low.getMinor() + 1, 0);
						VersionRange range = new VersionRange("[" + low + ","
								+ high + ")");

						Requirement r = new Requirement();
						r.ns = ns;
						r.description = "Require capability " + ns + ":" + name
								+ ";version=" + range;
						r.filter = "(&(" + ns + "=" + name + ")"
								+ range.toFilter() + ")";
						guard.requirements.add(r);
					}
				}
			}
		}

	}

	private Attrs getAttrs(Analyzer analyzer, String member) {
		Attrs pattrs = analyzer.getClasspathExports().getByFQN(member);
		if (pattrs == null || pattrs.isEmpty())
			pattrs = analyzer.getContained().getByFQN(member);
		if (pattrs == null || pattrs.isEmpty())
			pattrs = analyzer.getReferred().getByFQN(member);
		return pattrs;
	}

	/*
	 * Create a list of all services listed in the set attribute.
	 */

	private void doServices(Analyzer analyzer, Attrs attrs, Guard guard) {
		String set = attrs.get("set");
		if (set != null && !set.isEmpty()) {
			String[] members = set.trim().split("\\s*,\\s*");
			for (String member : members) {
				Requirement r = new Requirement();
				r.ns = "osgi.service";
				r.description = "Require service " + member;
				r.filter = "(&(osgi.service=" + member + ")";
				r.effective = "active";
				guard.requirements.add(r);
			}
		}
	}
}
