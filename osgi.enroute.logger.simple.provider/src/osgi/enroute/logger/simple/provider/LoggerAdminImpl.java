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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.lib.converter.Converter;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import osgi.enroute.debug.api.Debug;
import osgi.enroute.logger.api.Level;
import osgi.enroute.logger.api.LoggerAdmin;
import osgi.enroute.logger.api.LoggerConstants;
import osgi.enroute.logger.simple.provider.LoggerDispatcher.Eval;

/**
 * This is the Logger Admin component. It registers a {@link LoggerAdmin}
 * service.
 */
// @formatter:off
@ProvideCapability(ns=ImplementationNamespace.IMPLEMENTATION_NAMESPACE, name=LoggerConstants.LOGGER_SPECIFICATION_NAME, version=LoggerConstants.LOGGER_SPECIFICATION_VERSION)
@Designate(ocd=Configuration.class)
@Component(
		immediate = true,
		scope = ServiceScope.SINGLETON,
		service = LoggerAdmin.class,
		configurationPolicy = ConfigurationPolicy.OPTIONAL,
		property = {
				Debug.COMMAND_SCOPE + "=logger", 
				Debug.COMMAND_FUNCTION + "=match",
				Debug.COMMAND_FUNCTION + "=unmatch",
				Debug.COMMAND_FUNCTION + "=settings",
				Debug.COMMAND_FUNCTION + "=loggers",
				Debug.COMMAND_FUNCTION + "=slf4j",
				Debug.COMMAND_FUNCTION + "=error",
				Debug.COMMAND_FUNCTION + "=trace",
				Debug.COMMAND_FUNCTION + "=warning",
				Debug.COMMAND_FUNCTION + "=info"
		})
// @formatter:on
public class LoggerAdminImpl extends Thread implements LoggerAdmin, Eval {
	final static Logger	logger		= LoggerFactory.getLogger(LoggerAdminImpl.class);
	final static int	LOG_TRACE	= LogService.LOG_DEBUG + 1;

	boolean										traces;
	PrintStream									out				= System.err;
	final List<ServiceReference<LogService>>	logReferences	= new CopyOnWriteArrayList<>();
	final List<LogService>						logs			= new CopyOnWriteArrayList<>();
	final Map<Pattern, Control>					controls		= new ConcurrentHashMap<>();
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
	public void activate(Configuration c) throws Exception {
		assert c != null;

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
							case 0:
								log.log(take.level, take.message);
								break;

							case 1:
								log.log(take.level, take.message, take.exception);
								break;

							case 2:
								log.log(take.reference, take.level, take.message);
								break;

							case 3:
								log.log(take.reference, take.level, take.message, take.exception);
								break;
							}

						} catch (Exception e) {
							//
							// Hmm, not much we can do here ...
							// Since we're the logging subsystem
							//
							e.printStackTrace();
						}
					}
				} catch (InterruptedException e) {
					interrupt();
					return;
				}
		} catch (Exception e) {
			//
			// Hmm, not much we can do here ...
			//
			e.printStackTrace();
		} finally {
		}
	}

	private List<LogService> getLogs(Entry take) throws InvalidSyntaxException {
		if (take.source == null)
			return logs;

		List<LogService> logs = new ArrayList<>();
		BundleContext ctx = take.source.getBundleContext();
		if (ctx == null)
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
		for (java.util.Map.Entry<Pattern, Control> p : controls.entrySet()) {
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

	public List<Info> loggers(String glob) throws Exception {
		return list(glob);
	}
	
	public List<Info> loggers() throws Exception {
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
		Map<Pattern, Control> controls = new HashMap<>();

		for (Control c : settings.controls) {
			try {
				Pattern p = Glob.toPattern(c.pattern);
				controls.put(p, c);
			} catch (Exception ee) {
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
	public Control match(Glob pattern, String level, String... options) throws Exception {
		Settings settings = getSettings();
		Control control = new Control();
		control.level = toLevel(level);
		control.pattern = pattern.toString();
		for (String option : options) {
			switch (option) {
			case "stacktrace":
				control.stackTraces = true;
				break;

			case "where":
				control.where = true;
				break;

			default:
				System.err.println("Unknown option " + option);
				break;
			}
		}
		settings.controls.add(control);
		setSettings(settings);
		return control;
	}

	private Level toLevel(String level) throws Exception {
		return Converter.cnv(Level.class, level.toUpperCase());
	}

	/**
	 * Shell command to remove a control
	 * 
	 * @param pattern
	 *            the pattern to remove (will remove all controls with that
	 *            exact pattern)
	 * @return the controls removed
	 */
	public List<Control> unmatch(Glob pattern) throws Exception {
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

	/**
	 * Create a log event
	 * 
	 * @param log
	 * @throws Exception
	 */

	public void slf4j(String level, String... msg) throws Exception {
		Level l = toLevel(level);
		String s = Strings.join(" ", msg);
		if ( s.isEmpty())
			s = level;
		
		switch(l) {
		case AUDIT:
			logger.error("{}",s);
			break;
		case DEBUG:
			logger.debug("{}",s);
			break;
		default:
		case R1:
		case R2:
		case R3:
		case ERROR:
			logger.error("{}",s);
			break;
		case INFO:
			logger.info("{}",s);
			break;

		case TRACE:
			logger.trace("{}",s);
			break;
		case WARN:
			logger.warn("{}",s);
			break;
		}
	}

	/*
	 * Get the log services
	 */
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, service = LogService.class)
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
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
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
	
	public void error(String msg) {
		logger.error(msg);
	}
	public void info(String msg) {
		logger.info(msg);
	}
	public void trace(String msg) {
		logger.trace(msg);
	}
	public void warning(String msg) {
		logger.warn(msg);
	}
}
