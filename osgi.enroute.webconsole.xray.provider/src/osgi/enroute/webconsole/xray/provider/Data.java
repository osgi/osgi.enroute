package osgi.enroute.webconsole.xray.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The public Data objects used in this package.
 */
public interface Data {

	/**
	 * Holds component information
	 */
	class ComponentDef {
		public long			id;
		public boolean		unsatisfied;
		public boolean		enabled;
		public String		name;
		public Set<String>	references	= new HashSet<String>();
		public String[]		services;
		public int			index;
		public String missingRefs;
	}

	/**
	 * Holds Service information
	 */
	class ServiceDef {
		public String name;
		public int row = Integer.MAX_VALUE;
		public int column;
		public String shortName;
		public Integer[] registering;
		public Integer[] listening;
		public Integer[] getting;
		public Integer[] classspaces;
		public List<Long> ids = new ArrayList<Long>();
		public boolean exported;
		public boolean imported;

		/*
		 * Transient helpers to build up registering, listening and getting.
		 */
		transient List<BundleDef> r = new ArrayList<BundleDef>();
		transient List<BundleDef> g = new ArrayList<BundleDef>();
		transient List<BundleDef> l = new ArrayList<BundleDef>();
		transient Set<BundleDef> c = new HashSet<BundleDef>();

		boolean isOrphan() {
			return r.size() <= 1 && g.isEmpty() && l.size() <= 1;
		}
	}

	/**
	 * Holds Bundle information
	 */
	class BundleDef {
		public enum STATE {
			UNKNOWN, INSTALLED, RESOLVED, STARTING, ACTIVE, STOPPING, UNINSTALLED;
		}

		public long id;
		public String bsn;
		public String name;
		public int row;
		public STATE state;
		public List<ComponentDef> components = new ArrayList<>();
		public String log;
		public boolean errors;
		public int revisions;
		transient int index;
		transient int orphans;
	}

	class PackageDef {
		public String name;
		public int exporter;
		public int revision;
		public List<Integer> importing;
	}

	/**
	 * Holds the Result information
	 */
	class Result {
		public List<ServiceDef> services = new ArrayList<ServiceDef>();
		public List<BundleDef> bundles = new ArrayList<BundleDef>();
		public String root;
	}

}
