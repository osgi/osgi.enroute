package osgi.enroute.base.debug.provider;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import osgi.enroute.debug.api.Debug;

public class EnRouteCommands implements LogListener, BundleActivator {

	volatile boolean											watchlog	= Boolean.getBoolean("enRoute.watchlog");
	final AtomicReference<LogReaderService>						logreader	= new AtomicReference<>();
	private ServiceTracker<LogReaderService,LogReaderService>	lrs;
	private BundleContext										context;
	final List<LogEntry>										log			= new ArrayList<>();

	/**
	 * Show the services
	 * 
	 * @throws InvalidSyntaxException
	 */

	public Object lss() throws InvalidSyntaxException {
		return context.getAllServiceReferences(null, null);
	}

	/**
	 * Command to toggle log
	 * 
	 * @return
	 */
	public String watchlog() {
		return watchlog(!watchlog);
	}

	/**
	 * Command set log on/off
	 * 
	 * @return
	 */
	public String watchlog(boolean on) {
		if (watchlog != on) {
			watchlog = on;
		}
		return watchlog ? "on" : "off";
	}

	/**
	 * Show the log (reversed so the newest entries are at the bottom)
	 */

	public Object lg(int level) {
		LogReaderService lrs = logreader.get();
		if (lrs != null) {
			List<LogEntry> entries = new ArrayList<>(log.subList(Math.max(0, log.size() - 50), log.size()));
			Collections.reverse(entries);

			try (Formatter f = new Formatter()) {
				for (LogEntry e : entries) {
					if (e.getLevel() <= level) {
						f.format("%2$tH:%2$tM:%2$tS [%1$s] %3$04d %4$s", label(e.getLevel()), e.getTime(), e
								.getBundle().getBundleId(), e.getMessage());
						if (e.getException() != null) {
							StringWriter sw = new StringWriter();
							PrintWriter pw = new PrintWriter(sw);
							e.getException().printStackTrace(pw);
							pw.flush();
							f.format("%n%s%n", sw.toString());
						}
						f.format("%n");
					}
				}
				return f.toString();
			}
		}
		return "No log reader service";
	}

	private Object label(int level) {
		switch (level) {
			case LogService.LOG_DEBUG :
				return "D";
			case LogService.LOG_INFO :
				return "I";
			case LogService.LOG_WARNING :
				return "W";
			case LogService.LOG_ERROR :
				return "E";
		}
		return level + "?";
	}

	public Object lg() {
		return lg(LogService.LOG_WARNING);
	}

	public Object lg(String level) {
		switch (level) {
			case "debug" :
				return lg(LogService.LOG_DEBUG);
			case "info" :
				return lg(LogService.LOG_INFO);
			case "warn" :
				return lg(LogService.LOG_WARNING);
			case "error" :
				return lg(LogService.LOG_ERROR);
		}
		return lg(LogService.LOG_DEBUG);
	}

	/**
	 * Command to say a text
	 */
	volatile int	nsays;

	public void say(final Object message) throws Exception {
		nsays++;
		if (nsays > 2)
			return;

		Thread t = new Thread("speak") {
			public void run() {
				try {
					ScriptEngine engine = new ScriptEngineManager().getEngineByName("AppleScript");
					engine.eval("say \"" + message + "\"");
				}
				catch (Exception e) {
					// ignore
				}
				finally {
					nsays--;
				}
			}
		};
		t.start();
	}

	/**
	 * List all the exported packages
	 */
	public static class Export extends DTO {
		public String					name;
		public Version					version;
		public Map<Double,Set<Double>>	wires	= new HashMap<>();
	}

	public Object exports() {
		return exports("");
	}

	public Object exports(String prefix) {
		Map<String,Export> index = new TreeMap<>();
		boolean needRefresh = false;

		for (Bundle b : context.getBundles()) {
			int r = 0;
			for (BundleRevision br : b.adapt(BundleRevisions.class).getRevisions()) {
				BundleWiring wiring = br.getWiring();

				List<BundleWire> exports = wiring.getProvidedWires(PackageNamespace.PACKAGE_NAMESPACE);
				List<BundleCapability> capabilities = wiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);

				Double exportRevision = (double) br.getBundle().getBundleId();
				if (r > 0) {
					needRefresh = true;
					exportRevision += r / 10D;
				}

				for (BundleCapability bc : capabilities) {
					String packageName = (String) bc.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
					Version version = (Version) bc.getAttributes().get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
					String id = packageName + ";" + version;
					Export to = index.get(id);
					if (to == null) {
						to = new Export();
						to.name = packageName;
						to.version = version;

						index.put(id, to);
					}

					Set<Double> list = to.wires.get(exportRevision);
					if (list == null) {
						list = new TreeSet<>();
						to.wires.put(exportRevision, list);
					}
				}

				for (BundleWire w : exports) {
					String packageName = (String) w.getCapability().getAttributes()
							.get(PackageNamespace.PACKAGE_NAMESPACE);
					Version version = (Version) w.getCapability().getAttributes()
							.get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
					String id = packageName + ";" + version;
					Export to = index.get(id);
					if (to == null) {
						to = new Export();
						to.name = packageName;
						to.version = version;

						index.put(id, to);
					}

					BundleRevision ibr = w.getRequirer();
					int ir = w.getRequirer().getBundle().adapt(BundleRevisions.class).getRevisions().indexOf(ibr);
					Double importRevision = (double) ibr.getBundle().getBundleId();
					if (ir > 0)
						importRevision += ir / 10;

					Set<Double> list = to.wires.get(exportRevision);
					if (list == null) {
						list = new TreeSet<>();
						to.wires.put(exportRevision, list);
					}

					list.add(importRevision);
				}

				r++;
			}
		}
		if (needRefresh)
			System.err.println("Refresh is needed");

		try (Formatter f = new Formatter()) {
			String packageName = null;
			for (Export export : index.values()) {
				if (export.name.contains(prefix)) {
					String name = export.name.equals(packageName) ? "" : export.name;
					String warning = export.wires.size() > 1 ? "**" : "";
					String version = export.version.getMajor() + "." + export.version.getMinor() + "."
							+ export.version.getMicro();
					Iterator<Map.Entry<Double,Set<Double>>> it = export.wires.entrySet().iterator();
					Entry<Double,Set<Double>> first = it.next();

					f.format("%2s %-40s %-10s  %-5s -> %s%n", warning, name, version, first.getKey(), first.getValue());
					for (; it.hasNext();) {
						Entry<Double,Set<Double>> v = it.next();
						f.format("**                                                        %-8s -> %s%n", v.getKey(),
								v.getValue());
					}
				}
			}
			return f.toString();
		}
	}

	/**
	 * Just show a service
	 * 
	 * @throws InvalidSyntaxException
	 */

	public Object srv(ServiceReference< ? >... refs) throws InvalidSyntaxException {
		if (refs == null || refs.length == 0)
			return context.getServiceReferences((String) null, null);

		if (refs.length == 1)
			return refs[0];

		return refs;
	}

	/**
	 * Just check the environment for anything uncanny
	 */

	public String state() {
		WiringState ws = new WiringState(context);
		ws.verify();

		return null;
	}

	/*
	 * Check log for errors
	 */
	static long	lastspoken;

	@Override
	public void logged(LogEntry entry) {
		if (watchlog && entry.getLevel() <= LogService.LOG_WARNING) {
			try {
				if (System.currentTimeMillis() > lastspoken + 15000 &&  !entry.getMessage().contains("[silent]")) {
					say("error in bundle " + entry.getBundle().getBundleId());
					lastspoken = System.currentTimeMillis();
				}
				System.err.println(entry.getMessage());
				log.add(entry);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		this.lrs = new ServiceTracker<LogReaderService,LogReaderService>(context, LogReaderService.class, null) {
			@Override
			public LogReaderService addingService(ServiceReference<LogReaderService> lrs) {
				LogReaderService l = super.addingService(lrs);
				l.addLogListener(EnRouteCommands.this);
				logreader.set(l);
				return l;
			}

			@Override
			public void removedService(ServiceReference<LogReaderService> l, LogReaderService ll) {
				logreader.compareAndSet(ll, null);
			}
		};
		this.lrs.open();

		Hashtable<String,Object> map = new Hashtable<>();
		map.put(Debug.COMMAND_SCOPE, "enroute");
		map.put(Debug.COMMAND_FUNCTION, new String[] {
				"say", "lg", "watchlog", "lss", "srv", "exports"
		});
		context.registerService(Object.class, this, map);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		this.lrs.close();
	}

}
