package osgi.enroute.logger.simple.provider;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.slf4j.Marker;

import osgi.enroute.logger.api.Level;
import osgi.enroute.logger.api.LoggerAdmin.Control;

/**
 * This is the base class for the class that must log information. It fully
 * implements the slf4j Logger class and maps them to an {@link Entry} record
 * which is then queued via the static (yuck) dispatcher.
 */
class AbstractLogger implements org.slf4j.Logger {

	/*
	 * A pattern to discard classes in a stacktrace that we do not want to see
	 */
	// final static Pattern CUSTOM_CLASSES = Pattern
	// .compile("(?!com\\.sun|sun|java\\.|osgi\\.enroute\\.logger\\.provider)(.+\\.)+(.*)");

	String				name;
	Bundle				bundle;

	volatile boolean	info;
	volatile boolean	trace;
	volatile boolean	debug;
	volatile boolean	warn;
	volatile boolean	error;
	volatile boolean	exceptions;
	volatile boolean	where;

	boolean				init;
	boolean				registered;
	Level				level;

	AbstractLogger(Bundle bundle, String name) {
		this.bundle = bundle;
		this.name = name;
		reset();
	}

	AbstractLogger() {
		reset();
	}

	/*
	 * The init method is checked when a flag is set to true (done in reset()).
	 * The init method will set all flags correctly based on defaults, or, if
	 * there is a admin, from the settings in the admin. The init method returns
	 * true if the given level must be logged. I think this is the fastest
	 * possible way when you're not using a level. I.e. initially trace is set
	 * to true. At the first trace, init(TRACE) is called but this in general
	 * will set the trace flag to false. So the second time only the trace flag
	 * is checked. In general, extra work is only done when we actually are
	 * going to log a message.
	 */
	private synchronized boolean init(Level level) {
		if (init)
			return true;

		init = true;

		//
		// We need to register once with the LoggerDispatcher
		// so the admin can reset us when there are new settings
		//

		if (!registered) {
			LoggerDispatcher.dispatcher.register(this);
			registered = true;
		}

		LoggerAdminImpl admin = LoggerDispatcher.dispatcher.admin;

		if (admin != null) {
			//
			// We have an admin. So we actually get our settins
			// from this admin.
			//
			Control control = admin.getControl(this.name);
			debug = control.level.ordinal() <= Level.DEBUG.ordinal();
			info = control.level.ordinal() <= Level.INFO.ordinal();
			trace = control.level.ordinal() <= Level.TRACE.ordinal();
			warn = control.level.ordinal() <= Level.WARN.ordinal();
			error = control.level.ordinal() <= Level.ERROR.ordinal();
			level = control.level;
		} else {
			//
			// Default Defaults if no admin present. There is a bit of a race
			// condition since at this admin could have become active and reset
			// us
			// However, this is pretty rare and should be corrected on the next
			// call
			//
			debug = false;
			info = false;
			trace = false;
			warn = true;
			error = true;
		}

		return isLevel(level);
	}

	/*
	 * Next time this logger is used it will try to get its configuration again
	 */
	synchronized void reset() {
		init = false;
		debug = true;
		info = true;
		trace = true;
		warn = true;
		error = true;
	}

	/*
	 * Check if this level is set
	 */
	private boolean isLevel(Level level) {
		switch (level) {
			case DEBUG :
				return debug;
			case ERROR :
				return error;
			case INFO :
				return info;

			case TRACE :
				return trace;
			case WARN :
				return warn;

			case R1 :
			case R2 :
			case R3 :
			case AUDIT :
			default :
				return true;

		}
	}

	/*
	 * Return the name
	 */
	@Override
	public String getName() {
		return name;
	}

	/*
	 * The core routine. We've committed to logging so now we have to create a
	 * logging message.
	 */
	void message(int level, String format, Object... arguments) {
		try {
			//
			// Log4j is using {} as the %s ... :-(
			// so we replace it with the %s
			// TODO figure out markers
			//
			if (format.indexOf('{') >= 0) {
				format = format.replaceAll("\\{\\}", "%s");
			}

			//
			// We will log an entry to the queue
			//
			Entry entry = new Entry();
			entry.level = level;

			//
			// Adjust the arguments since arrays print badly and we can do
			// better for some other objects as well.
			//

			for (int i = 0; i < arguments.length; i++)
				if (arguments[i] != null) {

					if (entry.reference == null && arguments[i] instanceof ServiceReference< ? >) {
						entry.reference = (ServiceReference< ? >) arguments[i];
					} else if (entry.exception == null && arguments[i] instanceof Throwable) {
						entry.exception = (Throwable) arguments[i];
					} else if (!(arguments[i] instanceof String))
						arguments[i] = toString(arguments[i]);
				}

			//
			// Add a few more places so that errors in the format would refer to
			// non-existent args. Logging should not throw exceptions.
			//

			Object nargs[] = new Object[arguments.length + 10];
			System.arraycopy(arguments, 0, nargs, 0, arguments.length);

			final StringBuilder sb = new StringBuilder();
			try (Formatter formatter = new Formatter(sb)) {

				if (name != null) {
					sb.append(name).append(" :: ");
				}

				if (where) {
					where(sb, 4);
				}

				formatter.format(format, nargs);
				if (entry.exception != null && exceptions) {
					sb.append("\n");
					try (PrintWriter sw = getWriter(sb)) {
						entry.exception.printStackTrace(sw);
					}
				}
			}

			entry.message = sb.toString();
			entry.source = bundle;

			//
			// We will not block on the queue. So we attempt to put it in the
			// queue and if we do not succeed, print it to std err.
			//
			if (!LoggerDispatcher.dispatcher.queue.offer(entry)) {
				System.err.println("Overflowing log queue " + entry);
			}
		}
		catch (Exception e) {
			System.err.println("Shamefully have to admit the log service failed :-(" + e);
			e.printStackTrace();
		}
	}

	private PrintWriter getWriter(final StringBuilder sb) {
		return new PrintWriter(new Writer() {

			@Override
			public void write(char[] cbuf, int off, int len) throws IOException {
				for (int i = 0; i < len; i++)
					sb.append(cbuf[i + off]);
			}

			@Override
			public void flush() throws IOException {}

			@Override
			public void close() throws IOException {}

		});
	}

	/*
	 * Create a more suitable text presentation for array objects
	 * @param object
	 * @return
	 */
	private String toString(Object object) {
		if (object == null)
			return "null";

		if (object.getClass().isArray()) {
			StringBuilder sb = new StringBuilder();
			String del = "[";
			for (int i = 0; i < Array.getLength(object); i++) {
				sb.append(del).append(toString(Array.get(object, i)));
				del = ", ";
			}
			sb.append("]");
			return sb.toString();
		}
		return object.toString();
	}

	/*
	 * Get the current location of where the error was reported.
	 */
	protected void where(StringBuilder sb, int max) {
		try {
			throw new Exception();
		}
		catch (Exception e) {
			StackTraceElement[] stackTrace = e.getStackTrace();
			int n = 0;
			for (int i = 2; i < sb.length(); i++) {
				Matcher matcher = Pattern.compile(".*").matcher(stackTrace[i].getClassName());

				if (matcher.matches()) {
					String logMethod = stackTrace[i].getMethodName();
					String logClass = matcher.group(2);
					int line = stackTrace[i].getLineNumber();
					sb.append("[").append(logClass).append(".").append(logMethod);
					if (line != 0)
						sb.append(":").append(line);
					sb.append("] ");
					n++;
					if (n >= max)
						return;
				}
			}
		}
	}

	/**************************************************************************************************************/

	// The rest is the SLF4J dump ... what backward compatibility does to you
	// ... :-(

	@Override
	public void info(String format, Object... arguments) {
		if (info && init(Level.INFO))
			message(LogService.LOG_INFO, format, arguments);
	}

	@Override
	public void debug(String format, Object... arguments) {
		if (debug && init(Level.DEBUG))
			message(LogService.LOG_DEBUG, format, arguments);
	}

	@Override
	public void error(String format, Object... arguments) {
		if (error && init(Level.ERROR))
			message(LogService.LOG_ERROR, format, arguments);
	}

	@Override
	public void trace(String format, Object... arguments) {
		if (trace && init(Level.TRACE))
			message(LoggerAdminImpl.LOG_TRACE, format, arguments);
	}

	@Override
	public void warn(String format, Object... arguments) {
		if (warn && init(Level.WARN))
			message(LogService.LOG_WARNING, format, arguments);
	}

	@Override
	public boolean isInfoEnabled() {
		return info && init(Level.INFO);
	}

	@Override
	public boolean isDebugEnabled() {
		return debug && init(Level.DEBUG);
	}

	@Override
	public boolean isErrorEnabled() {
		return error && init(Level.ERROR);
	}

	@Override
	public boolean isTraceEnabled() {
		return trace && init(Level.TRACE);
	}

	@Override
	public boolean isWarnEnabled() {
		return warn && init(Level.WARN);
	}

	public void close() {
		LoggerDispatcher.dispatcher.unregister(this);
		init = registered = info = trace = error = warn = debug = false;
	}

	@Override
	public void debug(String string) {
		if (debug && init(Level.DEBUG))
			message(LogService.LOG_DEBUG, string);
	}

	@Override
	public void debug(String format, Object arguments) {
		if (debug && init(Level.DEBUG))
			message(LogService.LOG_DEBUG, format, arguments);
	}

	@Override
	public void debug(String string, Throwable t) {
		if (debug && init(Level.DEBUG))
			message(LogService.LOG_DEBUG, string, t);
	}

	@Override
	public void debug(Marker marker, String string) {
		if (debug && init(Level.DEBUG))
			message(LogService.LOG_DEBUG, string, marker);
	}

	@Override
	public void debug(String format, Object a, Object b) {
		if (debug && init(Level.DEBUG))
			message(LogService.LOG_DEBUG, format, a, b);
	}

	@Override
	public void debug(Marker marker, String format, Object a) {
		if (debug && init(Level.DEBUG))
			message(LogService.LOG_DEBUG, format, a, marker);
	}

	@Override
	public void debug(Marker marker, String format, Object... args) {
		if (debug && init(Level.DEBUG))
			message(LogService.LOG_DEBUG, format, args);
	}

	@Override
	public void debug(Marker marker, String format, Throwable t) {
		if (debug && init(Level.DEBUG))
			message(LogService.LOG_DEBUG, format, t);
	}

	@Override
	public void debug(Marker marker, String format, Object a, Object b) {
		if (debug && init(Level.DEBUG))
			message(LogService.LOG_DEBUG, format, a, b);
	}

	@Override
	public void error(String string) {
		if (error && init(Level.ERROR))
			message(LogService.LOG_ERROR, string);
	}

	@Override
	public void error(String format, Object arguments) {
		if (error && init(Level.ERROR))
			message(LogService.LOG_ERROR, format, arguments);
	}

	@Override
	public void error(String string, Throwable t) {
		if (error && init(Level.ERROR))
			message(LogService.LOG_ERROR, string, t);
	}

	@Override
	public void error(Marker marker, String string) {
		if (error && init(Level.ERROR))
			message(LogService.LOG_ERROR, string, marker);
	}

	@Override
	public void error(String format, Object a, Object b) {
		if (error && init(Level.ERROR))
			message(LogService.LOG_ERROR, format, a, b);
	}

	@Override
	public void error(Marker marker, String format, Object a) {
		if (error && init(Level.ERROR))
			message(LogService.LOG_ERROR, format, a, marker);
	}

	@Override
	public void error(Marker marker, String format, Object... args) {
		if (error && init(Level.ERROR))
			message(LogService.LOG_ERROR, format, args);
	}

	@Override
	public void error(Marker marker, String format, Throwable t) {
		if (error && init(Level.ERROR))
			message(LogService.LOG_ERROR, format, t);
	}

	@Override
	public void error(Marker marker, String format, Object a, Object b) {
		if (error && init(Level.ERROR))
			message(LogService.LOG_ERROR, format, a, b);
	}

	@Override
	public void info(String string) {
		if (info && init(Level.INFO))
			message(LogService.LOG_INFO, string);
	}

	@Override
	public void info(String format, Object arguments) {
		if (info && init(Level.INFO))
			message(LogService.LOG_INFO, format, arguments);
	}

	@Override
	public void info(String string, Throwable t) {
		if (info && init(Level.INFO))
			message(LogService.LOG_INFO, string, t);
	}

	@Override
	public void info(Marker marker, String string) {
		if (info && init(Level.INFO))
			message(LogService.LOG_INFO, string, marker);
	}

	@Override
	public void info(String format, Object a, Object b) {
		if (info && init(Level.INFO))
			message(LogService.LOG_INFO, format, a, b);
	}

	@Override
	public void info(Marker marker, String format, Object a) {
		if (info && init(Level.INFO))
			message(LogService.LOG_INFO, format, a, marker);
	}

	@Override
	public void info(Marker marker, String format, Object... args) {
		if (info && init(Level.INFO))
			message(LogService.LOG_INFO, format, args);
	}

	@Override
	public void info(Marker marker, String format, Throwable t) {
		if (info && init(Level.INFO))
			message(LogService.LOG_INFO, format, t);
	}

	@Override
	public void info(Marker marker, String format, Object a, Object b) {
		if (info && init(Level.INFO))
			message(LogService.LOG_INFO, format, a, b);
	}

	@Override
	public boolean isDebugEnabled(Marker arg0) {
		return debug && init(Level.DEBUG);
	}

	@Override
	public boolean isErrorEnabled(Marker arg0) {
		return error && init(Level.ERROR);
	}

	@Override
	public boolean isInfoEnabled(Marker arg0) {
		return info && init(Level.INFO);
	}

	@Override
	public boolean isTraceEnabled(Marker arg0) {
		return trace && init(Level.TRACE);
	}

	@Override
	public boolean isWarnEnabled(Marker arg0) {
		return warn && init(Level.WARN);
	}

	@Override
	public void warn(String string) {
		if (warn && init(Level.WARN))
			message(LogService.LOG_WARNING, string);
	}

	@Override
	public void warn(String format, Object arguments) {
		if (warn && init(Level.WARN))
			message(LogService.LOG_WARNING, format, arguments);
	}

	@Override
	public void warn(String string, Throwable t) {
		if (warn && init(Level.WARN))
			message(LogService.LOG_WARNING, string, t);
	}

	@Override
	public void warn(Marker marker, String string) {
		if (warn && init(Level.WARN))
			message(LogService.LOG_WARNING, string, marker);
	}

	@Override
	public void warn(String format, Object a, Object b) {
		if (warn && init(Level.WARN))
			message(LogService.LOG_WARNING, format, a, b);
	}

	@Override
	public void warn(Marker marker, String format, Object a) {
		if (warn && init(Level.WARN))
			message(LogService.LOG_WARNING, format, a, marker);
	}

	@Override
	public void warn(Marker marker, String format, Object... args) {
		if (warn && init(Level.WARN))
			message(LogService.LOG_WARNING, format, args);
	}

	@Override
	public void warn(Marker marker, String format, Throwable t) {
		if (warn && init(Level.WARN))
			message(LogService.LOG_WARNING, format, t);
	}

	@Override
	public void warn(Marker marker, String format, Object a, Object b) {
		if (warn && init(Level.WARN))
			message(LogService.LOG_WARNING, format, a, b);
	}

	@Override
	public void trace(String string) {
		if (trace && init(Level.TRACE))
			message(LoggerAdminImpl.LOG_TRACE, string);
	}

	@Override
	public void trace(String format, Object arguments) {
		if (trace && init(Level.TRACE))
			message(LoggerAdminImpl.LOG_TRACE, format, arguments);
	}

	@Override
	public void trace(String string, Throwable t) {
		if (trace && init(Level.TRACE))
			message(LoggerAdminImpl.LOG_TRACE, string, t);
	}

	@Override
	public void trace(Marker marker, String string) {
		if (trace && init(Level.TRACE))
			message(LoggerAdminImpl.LOG_TRACE, string, marker);
	}

	@Override
	public void trace(String format, Object a, Object b) {
		if (trace && init(Level.TRACE))
			message(LoggerAdminImpl.LOG_TRACE, format, a, b);
	}

	@Override
	public void trace(Marker marker, String format, Object a) {
		if (trace && init(Level.TRACE))
			message(LoggerAdminImpl.LOG_TRACE, format, a, marker);
	}

	@Override
	public void trace(Marker marker, String format, Object... args) {
		if (trace && init(Level.TRACE))
			message(LoggerAdminImpl.LOG_TRACE, format, args);
	}

	@Override
	public void trace(Marker marker, String format, Throwable t) {
		if (trace && init(Level.TRACE))
			message(LoggerAdminImpl.LOG_TRACE, format, t);
	}

	@Override
	public void trace(Marker marker, String format, Object a, Object b) {
		if (trace && init(Level.TRACE))
			message(LoggerAdminImpl.LOG_TRACE, format, a, b);
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
		if (this.name == null) {
			String name = this.bundle.getSymbolicName();
			if (name == null)
				name = bundle.getBundleId() + "";
			if (bundle.getVersion() != null)
				name += ";" + bundle.getVersion();

			setName(name);
		}
	}

	public void setName(String name) {
		this.name = name;
	}

}
