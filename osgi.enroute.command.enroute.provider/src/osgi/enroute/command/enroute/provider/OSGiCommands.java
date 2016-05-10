package osgi.enroute.command.enroute.provider;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.log.LogService;

import aQute.libg.glob.Glob;
import osgi.enroute.command.enroute.provider.LogTracker.Level;
import osgi.enroute.debug.api.Debug;
import osgi.enroute.dto.api.DTOs;
import osgi.enroute.dto.api.TypeReference;

/**
 * 
 */
@Component(service = Object.class, name = "osgi.enroute.command.enroute.osgi", property = { Debug.COMMAND_SCOPE + "=enroute",
		Debug.COMMAND_FUNCTION + "=log", Debug.COMMAND_FUNCTION + "=lss", Debug.COMMAND_FUNCTION + "=logerror",
		Debug.COMMAND_FUNCTION + "=start", Debug.COMMAND_FUNCTION + "=stop", Debug.COMMAND_FUNCTION + "=lsb", Debug.COMMAND_FUNCTION + "=lsc",
		Debug.COMMAND_FUNCTION + "=refresh" })
public class OSGiCommands {

	static class PackageDTO extends DTO {
		public String name;
		public String version;
		public Bundle exportedBy;
		public List<Bundle> importedBy = new ArrayList<>();
	}
	BundleContext						context		= FrameworkUtil.getBundle(OSGiCommands.class).getBundleContext();
	static TypeReference<List<String>>	stringlist	= new TypeReference<List<String>>() {
	};
	private LogTracker					logTracker;

	@Reference
	DTOs dtos;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	LogService log;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	ServiceComponentRuntime ds;

	@Activate
	void act(BundleContext context) {
		logTracker = new LogTracker(context);
		logTracker.open();
	}

	@Deactivate
	void deact(BundleContext context) {
		logTracker.close();
	}

	/**
	 * List services
	 * 
	 * @param v
	 *            use the glob as not
	 * @param bundleId
	 *            the bundle id to look for or -1 for all
	 * @param g
	 *            the glob expression
	 * @param keys
	 *            any keys that should be listed from the properties
	 * @return a list of strings
	 * @throws Exception
	 */
	@Descriptor("List services and optionally filter them for keywords. Specifying property keys limts the output to those keys.")
	public List<String> lss(//
			@Descriptor("Reverse they keyword filter match") //
			@Parameter(names = { "-v", "--not" }, presentValue = "true", absentValue = "false") boolean v, //
			@Descriptor("Only show services of the given bundle") @Parameter(names = { "-b",
					"--bundle" }, absentValue = "-1") long bundleId, //
			@Descriptor("filter") Glob g, @Descriptor("key") String... keys) throws Exception

	{
		ArrayList<String> sb = new ArrayList<>();
		ArrayList<Glob> columns = new ArrayList<>();
		for (String key : keys) {
			columns.add(new Glob(key));
		}

		ServiceReference<?>[] refs = context.getAllServiceReferences(null, null);
		if (refs != null) {
			for (ServiceReference<?> ref : refs) {

				if (bundleId >= 0 && ref.getBundle().getBundleId() != bundleId)
					continue;

				Formatter sub = new Formatter();
				try {
					String[] objectClass = (String[]) ref.getProperty(Constants.OBJECTCLASS);
					String del = ", ";
					for (String oc : objectClass) {
						sub.format("%-24s", toShortName(oc));
					}
					del = " : ";
					for (String key : ref.getPropertyKeys()) {

						if (columns.isEmpty() || has(columns, key)) {
							sub.format("%s%s=%s", del, key, toString(ref.getProperty(key)));
							del = ", ";
						}
					}
					String s = sub.toString();
					if (g.matcher(s).find() == !v)
						sb.add(s);
				} finally {
					sub.close();
				}
			}
		}
		return sb;
	}

	private String toShortName(String oc) {
		int n = oc.lastIndexOf('.');
		return oc.substring(n + 1);
	}

	private boolean has(ArrayList<Glob> columns, String key) {
		for (Glob g : columns) {
			if (g.matcher(key).matches())
				return true;
		}
		return false;
	}

	private String toString(Object o) throws Exception {
		if (o == null)
			return "null";

		return dtos.convert(o).to(String.class).toString();
	}

	@Descriptor("List all registered services")
	public List<String> lss() throws Exception {
		return lss(false, -1, new Glob("*"));
	}

	@Descriptor("List all services of the given bundle-id")
	public List<String> lss(@Descriptor("bundle-id") int bundleId) throws Exception {
		return lss(false, bundleId, new Glob("*"));
	}

	/**
	 * List log
	 * 
	 * @param reverse
	 * @param skip
	 * @param maxEntries
	 * @param level
	 * @param style
	 * @param noExceptions
	 * @return
	 * @throws IOException
	 */
	@Descriptor("Show the contents of the log")
	public List<String> log(
			//
			CommandSession session, @Descriptor("Tail the log") //
			@Parameter(names = { "-t", "--tail" }, presentValue = "true", absentValue = "false") boolean tail, //
			@Descriptor("Reverse the printout order to oldest is last") //
			@Parameter(names = { "-r", "--reverse" }, absentValue = "true", presentValue = "false") boolean reverse, //
			//
			@Descriptor("Skip the first entries") //
			@Parameter(names = { "-s", "--skip" }, absentValue = "0") int skip, //

	//
			@Descriptor("Maximum number of entries to print") //
			@Parameter(names = { "-m", "--max" }, absentValue = "100") int maxEntries, //
			@Descriptor("Minimum level (error,warning,info,debug). Default is warning.") //
			@Parameter(names = { "-l", "--level" }, absentValue = "warning") String level, //
			@Descriptor("Print style (classic,abbr)") //
			@Parameter(names = { "-y", "--style" }, absentValue = "classic") String style, //
			@Descriptor("Do not print exceptions.") @Parameter(names = { "-n",
					"--noexceptions" }, absentValue = "false", presentValue = "true") boolean noExceptions //
	) throws IOException {
		if (tail) {
			logtail(session);
			return null;
		}

		return logTracker.log(maxEntries, skip, toLogLevel(level), reverse, noExceptions,
				LogTracker.Style.valueOf(style));
	}

	private Level toLogLevel(String logLevel) {
		return LogTracker.Level.valueOf(logLevel.toLowerCase());
	}

	@Descriptor("Tail the log")
	public void logtail(CommandSession session) throws IOException {
		PrintStream console = session.getConsole();
		logTracker.addConsole(console);
		try {
			byte[] data = new byte[1];
			session.getKeyboard().read(data);
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			logTracker.removeConsole(console);
		}
	}

	@Descriptor("Generate an error in the log")
	public void logerror(@Descriptor("message") String foo) throws Exception {
		log.log(LogService.LOG_ERROR, foo);
	}

	@Descriptor("List bundles")
	public Collection<Bundle> lsb(
			@Descriptor("Show active bundles") @Parameter(names = { "-a",
					"--active" }, presentValue = "true", absentValue = "false") boolean active,
			@Descriptor("Show installed bundles") @Parameter(names = { "-i",
					"--installed" }, presentValue = "true", absentValue = "false") boolean installed,
			@Descriptor("Show uninstalled bundles") @Parameter(names = { "-r",
					"--resolved" }, presentValue = "true", absentValue = "false") boolean resolved,
			@Descriptor("Show uninstalled bundles") @Parameter(names = { "-u",
					"--uninstalled" }, presentValue = "true", absentValue = "false") boolean uninstalled,
			@Descriptor("Show starting/stopping bundles") @Parameter(names = { "-s",
					"--startingOrStopping" }, presentValue = "true", absentValue = "false") boolean changing

	) {
		int state = 0;
		if (active)
			state |= Bundle.ACTIVE;
		if (installed)
			state |= Bundle.INSTALLED;
		if (resolved)
			state |= Bundle.RESOLVED;
		if (uninstalled)
			state |= Bundle.UNINSTALLED;
		if (changing)
			state |= Bundle.STARTING | Bundle.STOPPING;

		if (state == 0)
			state = 0xFFFF_FFFF;
		final int effectiveState = state;

		return Stream.of(context.getBundles()).filter(b -> (b.getState() & effectiveState) != 0)
				.collect(Collectors.toList());
	}

	@Descriptor("Start one or more bundles, if none specified, start all non-active bundles.")
	public Collection<Bundle> start( //
			@Descriptor("Start option. 1=START_TRANSIENT and 2=START_ACTIVATION_POLICY, -1 no options") //
			@Parameter(names = { "-o", "--option" }, absentValue = "-1") int option, @Descriptor("id") long... ids) {
		List<Bundle> bundles = new ArrayList<>();
		Arrays.sort(ids);

		for (Bundle b : context.getBundles()) {
			if (b.getState() == Bundle.INSTALLED || b.getState() == Bundle.RESOLVED) {
				if (ids.length == 0 || in(ids, b.getBundleId())) {
					try {
						if (option > 0)
							b.start(option);
						else
							b.start();

						bundles.add(b);
					} catch (Exception e) {
						System.err.println(b + " : " + e.getMessage());
					}
				}
			}
		}

		return bundles;
	}

	@Descriptor("Stop one or more bundles, if none specified stop all active bundles")
	public Collection<Bundle> stop(@Descriptor("Start option. 1=STOP_TRANSIENT, -1 no options") //
	@Parameter(names = { "-o", "--option" }, absentValue = "-1") int option, //
			@Descriptor("id") long... ids) {
		List<Bundle> bundles = new ArrayList<>();
		Arrays.sort(ids);

		for (Bundle b : context.getBundles()) {
			if (b.getState() == Bundle.ACTIVE) {
				if (ids.length == 0 || in(ids, b.getBundleId())) {
					try {
						if (option > 0)
							b.stop(option);
						else
							b.stop();

						bundles.add(b);
					} catch (Exception e) {
						System.err.println(b + " : " + e.getMessage());
					}
				}
			}
		}

		return bundles;
	}

	@Descriptor("Refresh a set of bundles")
	public String refresh(CommandSession session,
			@Descriptor("Quiet") @Parameter(names = {"-q", "--quiet"}, absentValue = "false", presentValue = "true") boolean quiet,
			@Descriptor("Wait for the refresh to finish") @Parameter(names = {"-w", "--wait"}, absentValue = "false", presentValue = "true") boolean wait,
			Bundle ... bundles) {
		FrameworkWiring fw = context.getBundle(0).adapt(FrameworkWiring.class);
		List<Bundle> bs = Stream.of(bundles).collect(Collectors.toList());
		if (wait) {
			Semaphore s = new Semaphore(0);
			fw.refreshBundles(bs, (FrameworkEvent e) -> s.release());
			try {
				if (!quiet)
					session.getConsole().println("will wait for refresh to finish (max 3 mins)");
				s.tryAcquire(3, TimeUnit.MINUTES);
				if (!quiet)
					return "done";
			} catch (InterruptedException e1) {
				if (quiet)
					return null;
				return "Interrupted";
			}
		} else if (!quiet) {
			fw.refreshBundles(bs, (FrameworkEvent e) -> session.getConsole().println("Finished refreshing"));
			return "Will signal when refresh is done";
		}
		return null;
	}

	private boolean in(long[] ids, long id) {
		return Arrays.binarySearch(ids, id) >= 0;
	}

	public List<String> lsc() {
		return ds.getComponentDescriptionDTOs().stream().map(d -> d.name).collect(Collectors.toList());
	}

	public List<String> lsc(
			@Parameter(names = { "-c", "--configuration" }, absentValue = "false", presentValue = "true") boolean conf,
			@Parameter(names = { "-i", "--id" }, absentValue = "-1") long id, Glob g) {
		return ds.getComponentDescriptionDTOs().stream().filter(r -> g.matcher(r.name).matches()).map(d -> {
			Formatter sw = new Formatter();
			try {
				ComponentDescriptionDTO cdd = d;
				print(sw, cdd);
				if (conf) {
					Collection<ComponentConfigurationDTO> c = ds.getComponentConfigurationDTOs(d);
					for (ComponentConfigurationDTO ccd : c) {

						if (id >= 0 && id != ccd.id)
							continue;

						sw.format("--- %s ---\n", ccd.id);
						print(sw, ccd);
					}
					sw.format("---------------------\n");
				}
				return sw.toString();
			} finally {
				sw.close();
			}
		}).collect(Collectors.toList());
	}

	private void print(Formatter sw, DTO c) {
		for (Field f : c.getClass().getFields())

			try {
				if (Modifier.isStatic(f.getModifiers()))
					continue;

				sw.format("%-40s %s\n", toUpper(f.getName()), format(f.get(c)));
			} catch (Exception e) {
				sw.format("%-40s %s\n", toUpper(f.getName()), e.getMessage());
			}
	}

	private String format(Object object) {
		if (object.getClass().isArray())
			return Arrays.toString((Object[]) object);

		return object + "";
	}

	private Object toUpper(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

}
