package osgi.enroute.logger.simple.provider;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.log.LogService;
import osgi.enroute.debug.api.Debug;
import osgi.enroute.logger.api.Level;
import osgi.enroute.logger.api.LoggerAdmin;
import osgi.enroute.logger.capabilities.LoggerConstants;
import osgi.enroute.logger.simple.provider.LoggerDispatcher.Eval;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.libg.glob.Glob;

/**
 * This is the Logger Admin component. It registers a {@link LoggerAdmin}
 * service.
 */
@ProvideCapability(ns=ImplementationNamespace.IMPLEMENTATION_NAMESPACE, name=LoggerConstants.LOGGER_SPECIFICATION_NAME, version=LoggerConstants.LOGGER_SPECIFICATION_VERSION)
@Component(
		immediate = true,
		servicefactory = false,
		designate = Configuration.class,
		provide = LoggerAdmin.class,
		configurationPolicy = ConfigurationPolicy.optional,
		properties = {
				Debug.COMMAND_SCOPE + "=logger", Debug.COMMAND_FUNCTION + "=add|remove|settings|list"
		})
public class LoggerAdminImpl extends Thread implements LoggerAdmin, Eval {

	final static int							LOG_TRACE		= LogService.LOG_DEBUG + 1;

	boolean										traces;
	PrintStream									out				= System.err;
	final List<ServiceReference<LogService>>	logReferences	= new CopyOnWriteArrayList<>();
	final List<LogService>						logs			= new CopyOnWriteArrayList<>();
	final Map<Pattern,Control>					controls		= new ConcurrentHashMap<>();
	Control										control			= new Control();
	Settings									settings		= new Settings();
	JavaUtilLoggingHandler						javaUtilLogging;
	final CountDownLatch						latch			= new CountDownLatch(1);

	public LoggerAdminImpl() {
		super("OSGi :: Logger Admin");
		setDaemon(true);
	}

	/*
	 * Activate the component
	 */
	@Activate
	public void activate(Map<String,Object> config) throws Exception {
		assert config != null;

		Configuration c = Configurable.createConfigurable(Configuration.class, config);

		//
		// Check if we need java util logging
		//

		if (c.javaUtilLogging()) {
			java.util.logging.Logger.getLogger("").addHandler(javaUtilLogging = new JavaUtilLoggingHandler());
		}

		control.level = c.level() == null ? Level.WARN : c.level();
		control.thread = null;
		control.pattern = null;
		control.stackTraces = c.traces();
		control.where = c.where();

		//
		// Make us the admin ...
		//
		assert LoggerDispatcher.dispatcher.admin == null;

		LoggerDispatcher.dispatcher.admin = this;

		this.setPriority(Thread.MIN_PRIORITY);

		start();
	}

	@Deactivate
	public void deactivate() throws Exception {
		if (javaUtilLogging != null) {
			java.util.logging.Logger.getLogger("").removeHandler(javaUtilLogging);
		}
		//
		// Stop our thread
		//
		interrupt();
	}

	/*
	 * This is the queue flusher. We read from the queue and send it to the OSGi
	 * Log Services registered. We quit this method when we get an interrupt.
	 */
	public void run() {
		try {
			LoggerDispatcher.dispatcher.evaluate(this);

			//
			// Poll the queue until we get an interrupt
			//

			while (!isInterrupted())
				try {

					//
					// We wait util we get at least 1 log service. In the mean
					// time we buffer the messages anyway. This is one time
					// only.
					//

					latch.await(10, TimeUnit.SECONDS);

					Entry take = LoggerDispatcher.dispatcher.queue.take();

					//
					// If there are no log services we print to the
					// console
					//

					List<LogService> logs = getLogs(take);
					if (logs.isEmpty()) {
						System.err.println(take);
						continue;
					}

					//
					// We push to all log services registered since the deployer
					// can always set the x.target property to limit the
					// applicable log services
					//

					for (LogService log : logs) {

						try {

							int n = take.exception == null ? 0 : 1;
							n += take.reference == null ? 0 : 2;

							switch (n) {
								case 0 :
									log.log(take.level, take.message);
									break;

								case 1 :
									log.log(take.level, take.message, take.exception);
									break;

								case 2 :
									log.log(take.reference, take.level, take.message);
									break;

								case 3 :
									log.log(take.reference, take.level, take.message, take.exception);
									break;
							}

						}
						catch (Exception e) {
							//
							// Hmm, not much we can do here ...
							// Since we're the logging subsystem
							//
							e.printStackTrace();
						}
					}
				}
				catch (InterruptedException e) {
					interrupt();
					return;
				}
		}
		catch (Exception e) {
			//
			// Hmm, not much we can do here ...
			//
			e.printStackTrace();
		}
		finally {}
	}

	private List<LogService> getLogs(Entry take) throws InvalidSyntaxException {
		if (take.source == null)
			return logs;

		List<LogService> logs = new ArrayList<>();
		BundleContext ctx = take.source.getBundleContext();
		if ( ctx == null)
			return this.logs;
		
		for (ServiceReference<LogService> ref : ctx.getServiceReferences(LogService.class, null)) {
			LogService service = ctx.getService(ref);
			if (service != null)
				logs.add(service);
		}
		return logs;
	}

	/*
	 * We're an Eval, so we can iterate over the registered loggers. The default
	 * we supply is to reset the loggers so they have to refetch their
	 * configurations.
	 */
	public void eval(AbstractLogger msf) {
		msf.reset();
	}

	/*
	 * Called by an Abstract Logger when it is initing.
	 */
	Control getControl(String identifier) {
		for (java.util.Map.Entry<Pattern,Control> p : controls.entrySet()) {
			if (p.getKey().matcher(identifier).matches())
				return p.getValue();
		}
		return control;
	}

	/*
	 * List the information about the selected loggers (known to the system).
	 */
	@Override
	public List<Info> list(String glob) throws Exception {
		final List<Info> infos = new ArrayList<>();
		final Pattern p = glob == null ? null : Glob.toPattern(glob);
		;

		LoggerDispatcher.dispatcher.evaluate(new Eval() {

			@Override
			public void eval(AbstractLogger msf) {
				if (p == null || p.matcher(msf.name).find()) {
					Info info = new Info();
					info.bundleId = msf.bundle.getBundleId();
					info.level = msf.level;
					info.name = msf.name;
					infos.add(info);
				}
			}
		});
		return infos;
	}
	public List<Info> list() throws Exception {
		return list("*");
	}
	/*
	 * Get the current settings
	 */
	@Override
	public Settings getSettings() throws Exception {
		return settings;
	}

	/*
	 * Set the current settings and update the current loggers
	 */
	@Override
	public void setSettings(Settings settings) throws Exception {
		Map<Pattern,Control> controls = new HashMap<>();

		for (Control c : settings.controls) {
			try {
				Pattern p = Glob.toPattern(c.pattern);
				controls.put(p, c);
			}
			catch (Exception ee) {
				error("Invalid filter " + c.pattern, ee);
				return;
			}
		}
		synchronized (this.controls) {
			this.controls.clear();
			this.controls.putAll(controls);
		}
		LoggerDispatcher.dispatcher.evaluate(this);
		if (javaUtilLogging != null) {
			javaUtilLogging.update(controls);
		}
	}

	private void error(String string, Exception ee) {
		System.err.println(string);
	}

	/**
	 * Shell command to add a new setting
	 * 
	 * @param pattern
	 *            the pattern to add
	 * @param level
	 *            the level to report on
	 * @param options
	 *            set of options: stacktrace, where
	 * @return the Control added
	 */
	public Control add(Glob pattern, Level level, String... options) throws Exception {
		Settings settings = getSettings();
		Control control = new Control();
		control.level = level;
		control.pattern = pattern.toString();
		for (String option : options) {
			switch (option) {
				case "stacktrace" :
					control.stackTraces = true;
					break;

				case "where" :
					control.where = true;
					break;

				default :
					System.err.println("Unknown option " + option);
					break;
			}
		}
		settings.controls.add(control);
		setSettings(settings);
		return control;
	}

	/**
	 * Shell command to remove a control
	 * 
	 * @param pattern
	 *            the pattern to remove (will remove all controls with that
	 *            exact pattern)
	 * @return the controls removed
	 */
	public List<Control> remove(Glob pattern) throws Exception {
		Settings settings = getSettings();
		List<Control> deleted = new ArrayList<>();

		for (Iterator<Control> i = settings.controls.iterator(); i.hasNext();) {
			Control c = i.next();
			if (c.pattern.equals(pattern.toString())) {
				i.remove();
				deleted.add(c);
			}
		}
		setSettings(settings);
		return deleted;
	}

	/*
	 * Get the log services
	 */
	@Reference(type = '*', service = LogService.class)
	void addLog(ServiceReference<LogService> log) {
		latch.countDown();
		logReferences.add(log);
	}

	/*
	 * Remove the log services
	 */
	void removeLog(ServiceReference<LogService> log) {
		logReferences.remove(log);
	}

	/*
	 * Get the log services
	 */
	@Reference(type = '*')
	void addLogService(LogService log) {
		latch.countDown();
		logs.add(log);
	}

	/*
	 * Remove the log services
	 */
	void removeLogService(LogService log) {
		logs.remove(log);
	}
}
