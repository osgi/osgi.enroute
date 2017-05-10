package osgi.enroute.webconsole.xray.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import aQute.lib.collections.MultiMap;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import osgi.enroute.bostock.d3.webresource.capabilities.RequireD3Webresource;
import osgi.enroute.webconsole.xray.provider.Data.BundleDef;
import osgi.enroute.webconsole.xray.provider.Data.BundleDef.STATE;
import osgi.enroute.webconsole.xray.provider.Data.ComponentDef;
import osgi.enroute.webconsole.xray.provider.Data.Result;
import osgi.enroute.webconsole.xray.provider.Data.ServiceDef;
import osgi.enroute.webserver.capabilities.RequireWebServerExtender;

/**
 * This is a servlet that provides the status of the OSGi framework in a JSON
 * file. The state is represented as a {@link Result} data object. The servlet
 * is designed to work inside the Felix Web Console plugin model. This is a
 * final class because we assume resources are in the package so subclassing
 * might kill this.
 */

@RequireWebServerExtender
@RequireD3Webresource(resource = "d3.min.js")
public final class XRayWebPlugin extends AbstractWebConsolePlugin implements BundleActivator {
	private static final long serialVersionUID = 1L;
	private static String PLUGIN_NAME = "xray";
	private static String PREFIX = "/" + PLUGIN_NAME + "/";
	final static int TITLE_LENGTH = 14;
	final static Pattern LISTENER_INFO_PATTERN = Pattern.compile("\\(objectClass=([^)]+)\\)");
	final static JSONCodec codec = new JSONCodec();

	private BundleContext context;
	private LogReaderService logReader;
	private LogService log;
	private MultiMap<String, BundleContext> listenerContexts = new MultiMap<String, BundleContext>();
	private ServiceRegistration<ListenerHook> lhook;
	private volatile ServiceComponentRuntime scr;
	private volatile ConfigurationAdmin cfg;
	private volatile boolean quiting = false;

	/*
	 * Called at startup
	 */

	public void activate(BundleContext context) {
		super.activate(context);
		this.context = context;

		/*
		 * Register a ListenerHook to find out about any services that are
		 * searched for.
		 */
		lhook = context.registerService(ListenerHook.class, new ListenerHook() {

			public synchronized void added(Collection<ListenerInfo> listeners) {
				if (quiting)
					return;
				for (Object o : listeners) {
					addListenerInfo((ListenerInfo) o);
				}
			}

			public synchronized void removed(Collection<ListenerInfo> listeners) {
				if (quiting)
					return;
				for (Object o : listeners) {
					removeListenerInfo((ListenerInfo) o);
				}
			}
		}, null);
	}

	/*
	 * Called at going down
	 */
	public void deactivate(BundleContext context) {
		quiting = true;
		lhook.unregister();
		super.deactivate();
	}

	/**
	 * Required by the WebConsolePlugin
	 */
	@Override
	public String getLabel() {
		return PLUGIN_NAME;
	}

	/**
	 * Required by the WebConsolePlugin
	 */
	@Override
	public String getTitle() {
		return "X-Ray";
	}

	/**
	 * We need to be able to serve the state easily. So we implement the doGet
	 * and let the superclass handle anything but the state data.
	 */
	public void doGet(HttpServletRequest rq, HttpServletResponse rsp) throws ServletException, IOException {
		String path = rq.getPathInfo();
		if (path.endsWith("/state.json"))
			getState(rq, rsp);
		else if (path.endsWith("/config.json"))
			getConfig(rq, rsp);
		else if (path.endsWith("/command"))
			doCommand(rq, rsp);
		else if (path.startsWith(PREFIX)) {

			URL resource = getResource(path);
			if (resource == null) {
				rsp.sendError(HttpServletResponse.SC_NOT_FOUND, "The path " + path + " cannot be found");
			}

			String mime = getDefaultMime(path);
			if (mime != null)
				rsp.setContentType(mime);
			IO.copy(resource.openStream(), rsp.getOutputStream());

		} else
			try {
				super.doGet(rq, rsp);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	private String getDefaultMime(String path) {
		if (path != null) {
			int n = path.lastIndexOf('.');
			if (n > 0) {
				String suffix = path.substring(n + 1).toLowerCase();

				if (suffix.equals("css"))
					return "text/css";
				if (suffix.equals("js"))
					return "application/javascript";
			}
		}
		return null;
	}

	/**
	 * Execute a command
	 * 
	 * @param rq
	 * @param rsp
	 */
	private void doCommand(HttpServletRequest rq, HttpServletResponse rsp) {
		String cmd = rq.getParameter("c");
		if (cmd == null)
			throw new IllegalArgumentException("No c parameter set");

		if ("startall".equals(cmd)) {
			for (Bundle b : context.getBundles()) {
				try {
					b.start();
				} catch (BundleException be) {
					// Ignore
				}
			}
			return;
		}
		throw new IllegalArgumentException("Unknown command " + cmd);
	}

	/**
	 * Create a JSON file.
	 * 
	 * @param rq
	 * @param rsp
	 */
	private void getConfig(HttpServletRequest rq, HttpServletResponse rsp) {
		ConfigurationAdmin cfg = this.cfg;
		try {
			if (cfg != null) {
				Configuration cfgs[] = cfg.listConfigurations(null);
				ArrayList<Map<String, Object>> output = new ArrayList<Map<String, Object>>();
				if (cfgs != null) {
					for (Configuration config : cfgs) {
						Map<String, Object> map = new HashMap<String, Object>();

						Dictionary<String, Object> d = config.getProperties();
						if (d != null) {
							Enumeration<String> e = d.keys();
							while (e.hasMoreElements()) {
								String key = e.nextElement();
								Object value = d.get(key);
								map.put(key, value);
							}
						}
						output.add(map);
					}
				}
				rsp.setContentType("application/json");
				rsp.setCharacterEncoding("utf-8");
				OutputStream out = rsp.getOutputStream();
				codec.enc().charset("utf-8").to(out).writeDefaults().put(output).flush();
				out.close();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create the state file.
	 */
	private void getState(HttpServletRequest rq, HttpServletResponse rsp) {
		try {
			String c = rq.getParameter("cmd");
			if ("startall".equals(c)) {
				for (Bundle b : context.getBundles()) {
					try {
						b.start();
					} catch (BundleException be) {
						//
					}
				}
			}
			// Can specify service names to ignore
			String[] services = rq.getParameterValues("ignore");

			Result result = build(services);
			result.root = rq.getContextPath() + rq.getServletPath();

			codec.enc().to(rsp.getWriter()).writeDefaults().put(result);

		} catch (Exception e) {
			e.printStackTrace();
			log.log(LogService.LOG_ERROR, "Failed to create state file", e);
		}
	}

	/**
	 * Required by the Web Console Plugin to render an content. In out idea this
	 * is just the content of the content area of the webconsole. The Web
	 * Console is adding headers etc.
	 */
	@Override
	protected void renderContent(HttpServletRequest rq, HttpServletResponse rsp) throws ServletException, IOException {
		IO.copy(getClass().getResourceAsStream("/web/index.html"), rsp.getWriter());
	}

	/**
	 * Includes for the head element.
	 */
	@Override
	public String[] getCssReferences() {
		return new String[] { "/" + PLUGIN_NAME + "/style.css" };
	}

	/**
	 * Standard referring to statics. All resources should be in this package.
	 */
	public URL getResource(String resource) {
		if (resource.equals("/" + PLUGIN_NAME))
			return null;

		resource = resource.replaceAll("/" + PLUGIN_NAME + "/", "");
		URL url = getClass().getResource("/web/" + resource);
		return url;
	}

	/**
	 * Build up the graph
	 * 
	 * @param ignoredServices
	 * @throws InvalidSyntaxException
	 */
	Result build(String[] ignoredServices) throws InvalidSyntaxException {
		Map<String, ServiceDef> services = new TreeMap<String, ServiceDef>();
		// Set<PackageDef> packages = new LinkedHashSet<PackageDef>();
		Map<Bundle, BundleDef> bundles = new LinkedHashMap<Bundle, BundleDef>();

		Bundle[] bs = context.getBundles();
		int index = 0;
		for (Bundle bundle : bs) {
			BundleDef data = data(bundle);
			data.index = index++;
			bundles.put(bundle, data);
		}

		for (String name : new HashSet<String>(listenerContexts.keySet())) {
			ServiceDef icon = services.get(name);
			if (icon == null) {
				icon = new ServiceDef();
				services.put(name, icon);
				icon.name = name;
				icon.shortName = from(TITLE_LENGTH, name);
			}

			List<Bundle> listeners = getListeners(name);
			for (Bundle b : listeners)
				doClassspace(bundles, b, name, icon);

			for (Iterator<Bundle> i = listeners.iterator(); i.hasNext();) {
				Bundle b = i.next();
				BundleDef bdef = bundles.get(b);
				if (bdef == null)
					i.remove();
				else
					icon.l.add(bdef);
			}
		}

		for (ServiceReference<?> reference : context.getServiceReferences((String) null, null)) {

			for (String name : (String[]) reference.getProperty("objectClass")) {
				ServiceDef service = services.get(name);
				if (service == null) {
					service = new ServiceDef();
					services.put(name, service);
					service.name = name;
					service.shortName = from(TITLE_LENGTH, name);
				}
				service.exported = reference.getProperty("service.exported.interfaces") != null;
				service.imported = reference.getProperty("service.imported") != null;
				service.ids.add((Long) reference.getProperty("service.id"));
				if (reference.getUsingBundles() != null)
					for (Bundle b : reference.getUsingBundles()) {
						service.g.add(bundles.get(b));
					}

				service.r.add(bundles.get(reference.getBundle()));

				doClassspace(bundles, reference.getBundle(), name, service);
			}
		}

		if (ignoredServices != null)
			for (String s : ignoredServices)
				services.remove(s);

		layoutBundleFirst(bundles.values(), services.values());

		boolean[][] occupied = new boolean[bundles.size() + 1][services.size()];
		for (ServiceDef service : services.values()) {
			if (service.column > 0) {
				while (occupied[service.row][service.column])
					service.row++;
			}
			if (service.row < occupied.length && service.column < occupied[service.row].length)
				occupied[service.row][service.column] = true;
		}

		// Convert references to indexes
		for (ServiceDef sd : services.values()) {
			sd.registering = toIndexArray(sd.r);
			sd.listening = toIndexArray(sd.l);
			sd.getting = toIndexArray(sd.g);
			sd.classspaces = toIndexArray(sd.c);
		}

		Result result = new Result();
		result.bundles = new ArrayList<BundleDef>(bundles.values());
		result.services = new ArrayList<ServiceDef>(services.values());

		return result;
	}

	/**
	 * @param bundles
	 * @param reference
	 * @param name
	 * @param service
	 */
	protected void doClassspace(Map<Bundle, BundleDef> bundles, Bundle bundle, String name, ServiceDef service) {
		//
		// We want to the exporters of this class to display
		// the case when you've got multiple exporters
		//
		try {
			Class<?> serviceClass = bundle.loadClass(name);
			Bundle b = FrameworkUtil.getBundle(serviceClass);
			if (b == null)
				b = context.getBundle(0);
			service.c.add(bundles.get(b));
		} catch (Exception e) {
			// cannot happen
		}
	}

	private Integer[] toIndexArray(Collection<BundleDef> bs) {
		Integer[] result = new Integer[bs.size()];
		Iterator<BundleDef> it = bs.iterator();
		for (int i = 0; i < result.length; i++) {
			BundleDef next = it.next();
			if ( next != null)
				result[i] = next.index;
			else
				result[i] = -1;
		}
		return result;
	}

	private void layoutBundleFirst(Collection<BundleDef> bundles, Collection<ServiceDef> services) {
		LinkedList<BundleDef> bs = new LinkedList<BundleDef>(bundles);
		LinkedList<ServiceDef> ss = new LinkedList<ServiceDef>(services);

		int orphanStart = services.size();
		int column = 0;
		for (ServiceDef sd : services)
			if (sd.isOrphan())
				orphanStart--;

		int row = 0;

		while (!bs.isEmpty()) {
			BundleDef bd = bs.remove(0);
			bd.row = row++;
			LinkedHashSet<BundleDef> related = new LinkedHashSet<Data.BundleDef>();

			for (Iterator<ServiceDef> i = ss.iterator(); i.hasNext();) {
				ServiceDef sd = i.next();
				if (sd.r.contains(bd)) {
					sd.row = bd.row;
					sd.column = sd.isOrphan() ? orphanStart + bd.orphans++ : column++;
					related.addAll(sd.l);
					related.addAll(sd.g);
					related.addAll(sd.r);
					i.remove();
				} else if (sd.l.contains(bd)) {
					sd.row = bd.row;
					sd.column = sd.isOrphan() ? orphanStart + bd.orphans++ : column++;
					i.remove();
				}
			}
			for (BundleDef b : related) {
				if (bs.remove(b)) {
					bs.add(0, b);
				}
			}
		}
		for (BundleDef bd : bs) {
			bd.row = row++;
		}

		for (ServiceDef sd : services) {
			int max = findMaxRow(sd.r, sd.row);
			max = findMaxRow(sd.l, max);
			max = findMaxRow(sd.g, max);
			sd.row = sd.row + (max - sd.row + 1) / 2;
		}
	}

	private int findMaxRow(List<BundleDef> bs, int row) {
		for (BundleDef bd : bs) {
			if (bd != null)
				row = Math.max(bd.row, row);
		}
		return row;
	}

	/**
	 * Try to construct a readable name from a fqn that is likely too long
	 */
	String from(int n, String... strings) {
		for (String s : strings) {
			if (s != null) {
				if (s.length() > n) {
					s = s.substring(s.lastIndexOf('.') + 1);
					if (s.length() > n) {
						s = s.substring(s.lastIndexOf('.') + 1);
						if (s.length() > n) {
							if (s.endsWith("Listener"))
								s = s.substring(0, s.length() - "istener".length()) + ".";
							else if (s.endsWith("Service"))
								s = s.substring(0, s.length() - "ervice".length()) + ".";
							if (s.length() > n) {
								StringBuilder sb = new StringBuilder();
								for (int i = 0; i < s.length() && sb.length() < n; i++) {
									if ("aeiouy".indexOf(s.charAt(i)) < 0)
										sb.append(s.charAt(i));
								}
								s = sb.toString();
							}
							if (s.length() > n) {
								s = s.substring(0, 12) + "..";
							}
						}
					}
				}
				return s;
			}
		}
		return "<>";
	}

	/**
	 * Create the Bundle Definition
	 */
	private BundleDef data(Bundle bundle) {
		BundleDef bd = new BundleDef();
		bd.id = bundle.getBundleId();
		try {
			bd.bsn = bundle.getSymbolicName();
			bd.name = shortBsn(14, bd.bsn);

			switch (bundle.getState()) {
			case Bundle.INSTALLED:
				bd.state = STATE.INSTALLED;
				break;
			case Bundle.RESOLVED:
				bd.state = STATE.RESOLVED;
				break;
			case Bundle.STARTING:
				bd.state = STATE.STARTING;
				break;
			case Bundle.STOPPING:
				bd.state = STATE.STOPPING;
				break;
			case Bundle.UNINSTALLED:
				bd.state = STATE.UNINSTALLED;
				break;
			case Bundle.ACTIVE:
				bd.state = STATE.ACTIVE;
				break;

			default:
				bd.state = STATE.UNKNOWN;
				break;
			}

			if (bundle.getState() == Bundle.STARTING || bundle.getState() == Bundle.ACTIVE) {
				if (scr != null)
					doComponents(bundle, bd);
			}

			if (logReader != null)
				doLog(bundle, bd);

			BundleRevisions revisions = bundle.adapt(BundleRevisions.class);
			if (revisions != null) {
				List<BundleRevision> list = revisions.getRevisions();
				bd.revisions = list.size();
			}
		} catch (Exception e) {
			e.printStackTrace();
			bd.errors = true;
			bd.log = "Exception while retrieving data from framework " + e;
			bd.bsn = bd.name = "?";
			bd.state = STATE.UNKNOWN;
		}
		return bd;
	}

	private String shortBsn(int n, String bsn) {
		if (bsn.length() < n)
			return bsn;

		int nn = bsn.length();
		String[] split = bsn.split("\\.");
		StringBuilder sb = new StringBuilder(".");
		for (int i = 1; i < split.length; i++) {
			nn -= split[i].length() + 1;
			if (nn < n) {
				sb.append(".").append(split[i]);
			}
		}
		return sb.toString();
	}

	/**
	 * Use the ServiceComponentRuntime to create the components
	 */
	private void doComponents(Bundle bundle, BundleDef bd) {
		if (bundle == null || bundle.getState() == Bundle.UNINSTALLED)
			return;

		for (ComponentDescriptionDTO description : scr.getComponentDescriptionDTOs(bundle)) {
			Collection<ComponentConfigurationDTO> configs = scr.getComponentConfigurationDTOs(description);
			for (ComponentConfigurationDTO config : configs) {
				ComponentDef cdef = new ComponentDef();
				cdef.enabled = true;
				cdef.id = config.id;
				cdef.name = description.name;
				cdef.references = new HashSet<>();

				cdef.services = description.serviceInterfaces;
				for ( String service : cdef.services) {
					cdef.references.add( "=>" + service);
				}
				for (SatisfiedReferenceDTO ref : config.satisfiedReferences) {
					Set<Long>	services = new TreeSet<>();
					for ( ServiceReferenceDTO sref : ref.boundServices) {
						services.add( sref.id);
					}
					cdef.references.add( ref.name + "<=︎ " + services);
				}
				for (UnsatisfiedReferenceDTO ref : config.unsatisfiedReferences) {
					cdef.references.add( ref.name +"?" + ref.target);
					cdef.unsatisfied=true;
				}
				
				bd.components.add(cdef);
			}

		}
	}

	/**
	 * Use the LogReaderService to find out about log messages
	 */
	private void doLog(Bundle bundle, BundleDef bd) {
		@SuppressWarnings("unchecked")
		Enumeration<LogEntry> e = logReader.getLog();
		StringBuilder sb = new StringBuilder();
		Formatter f = new Formatter(sb);

		while (e.hasMoreElements()) {
			LogEntry entry = e.nextElement();
			if (entry.getBundle() == bundle) {
				if (entry.getTime() + 2 * 60 * 1000 > System.currentTimeMillis()) {
					if (entry.getLevel() <= LogService.LOG_WARNING) {
						String message = "";
						if (entry.getException() != null) {
							StringWriter sw = new StringWriter();
							PrintWriter pw = new PrintWriter(sw);

							Throwable t = entry.getException();
							while (t instanceof InvocationTargetException) {
								pw.println(t.getMessage());
								t = ((InvocationTargetException) t).getTargetException();
							}

							pw.println(t.getMessage());
							t.printStackTrace(pw);
							pw.flush();
							message = sw.toString();
						}
						f.format("%s:%s %s\n", entry.getLevel() == LogService.LOG_WARNING ? "W" : "E",
								entry.getMessage(), message);
						if (entry.getLevel() == LogService.LOG_WARNING)
							bd.errors |= true;
					}
				}
			}
		}
		bd.log = sb.toString();
		f.close();
	}

	// @Reference(type = '?')
	synchronized void setLogReader(LogReaderService log) {
		this.logReader = log;
	}

	synchronized void unsetLogReader(LogReaderService log) {
		if (logReader == log)
			logReader = null;
	}

	// @Reference(type = '?')
	synchronized void setLog(LogService log) {
		this.log = log;
	}

	synchronized void unsetLog(LogService log) {
		if (this.log == log)
			this.log = null;
	}

	// @Reference(type = '?')
	synchronized void setScr(ServiceComponentRuntime scr) {
		this.scr = scr;
	}

	synchronized void unsetScr(ServiceComponentRuntime scr) {
		if (this.scr == scr)
			this.scr = null;
	}

	// @Reference(type = '?')
	synchronized void setCfg(ConfigurationAdmin cfg) {
		this.cfg = cfg;
	}

	synchronized void unsetCfg(ConfigurationAdmin cfg) {
		if (this.cfg == cfg)
			this.cfg = cfg;
	}

	private synchronized List<Bundle> getListeners(String name) {
		List<BundleContext> namedContexts = listenerContexts.get(name);
		List<Bundle> listeners;
		
		if (namedContexts == null) {
			listeners = Collections.emptyList();
		} else {
			listeners = new ArrayList<Bundle>(namedContexts.size());
			for (BundleContext namedContext : namedContexts) {
				listeners.add(namedContext.getBundle());
			}
		}

		return listeners;
	}

	private synchronized void addListenerInfo(ListenerInfo o) {
		String filter = o.getFilter();
		if (filter != null) {
			Matcher m = LISTENER_INFO_PATTERN.matcher(filter);
			while (m.find()) {
				listenerContexts.add(m.group(1), o.getBundleContext());
			}
		}
	}

	private synchronized void removeListenerInfo(ListenerInfo o) {
		String filter = o.getFilter();
		if (filter != null) {
			Matcher m = LISTENER_INFO_PATTERN.matcher(filter);
			while (m.find()) {
				listenerContexts.remove(m.group(1), o.getBundleContext());
			}
		}
	}

	@Override
	public void start(BundleContext context) throws Exception {

		try {
			ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> cmt = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(
					context, ConfigurationAdmin.class.getName(), null) {
				public ConfigurationAdmin addingService(ServiceReference<ConfigurationAdmin> ref) {
					ConfigurationAdmin cm = context.getService(ref);
					setCfg(cm);
					return cm;
				}

				public void removedService(ServiceReference<ConfigurationAdmin> ref, ConfigurationAdmin s) {
					unsetCfg(s);
				}
			};
			cmt.open();
		} catch (Throwable e) {
			// Ignore, might not have the class
		}

		try {
			ServiceTracker<LogService, LogService> logt = new ServiceTracker<LogService, LogService>(context,
					LogService.class.getName(), null) {
				public LogService addingService(ServiceReference<LogService> ref) {
					LogService s = context.getService(ref);
					setLog(s);
					return s;
				}

				public void removedService(ServiceReference<LogService> ref, LogService s) {
					unsetLog(s);
				}
			};
			logt.open();
		} catch (Throwable e) {
			// Ignore, might not have the class
		}

		try {
			ServiceTracker<LogReaderService, LogReaderService> rdrt = new ServiceTracker<LogReaderService, LogReaderService>(
					context, LogReaderService.class.getName(), null) {
				public LogReaderService addingService(ServiceReference<LogReaderService> ref) {
					LogReaderService s = context.getService(ref);
					setLogReader(s);
					return s;
				}

				public void removedService(ServiceReference<LogReaderService> ref, LogReaderService s) {
					unsetLogReader(s);
				}
			};
			rdrt.open();
		} catch (Throwable e) {
			// Ignore, might not have the class
		}

		try {
			ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> scrt = new ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime>(
					context, ServiceComponentRuntime.class.getName(), null) {
				public ServiceComponentRuntime addingService(ServiceReference<ServiceComponentRuntime> ref) {
					ServiceComponentRuntime s = context.getService(ref);
					setScr(s);
					return s;
				}

				public void removedService(ServiceReference<ServiceComponentRuntime> ref, ServiceComponentRuntime s) {
					unsetScr(s);
				}
			};
			scrt.open();
		} catch (Throwable e) {
			// Ignore, might not have the class
		}
		Hashtable<String, Object> map = new Hashtable<String, Object>();
		map.put("felix.webconsole.label", "xray");
		context.registerService(Servlet.class, this, map);

		activate(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		deactivate();
	}
}