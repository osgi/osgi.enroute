package osgi.enroute.logger.simple.provider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

import org.osgi.service.log.LogService;

import osgi.enroute.logger.api.LoggerAdmin.Control;

/**
 * Logging is so much fun! Everybody should have their own logging API!
 * <p>
 * Anyway, this class links into the (quite horrible) Java Util Logging API and
 * forwards the messages to the OSGi Log Service.
 */

class JavaUtilLoggingHandler extends Handler {

	/*
	 * Loggers have a name. So we maintain a cache of the last 1000 loggers
	 */

	private final Map<String,AbstractLogger>	loggers	= new LinkedHashMap<String,AbstractLogger>() {
															private static final long	serialVersionUID	= 1L;

															protected boolean removeEldestEntry(
																	Map.Entry<String,AbstractLogger> eldest) {
																if (size() > 1000) {
																	eldest.getValue().close();
																}
																return false;
															}
														};

	/*
	 * Java util logging is a bit confusing and horribly designed. The Logger
	 * class is quite messed up with the reporting (slf4j is much better in that
	 * way). This publish method has already passed any filters (which are
	 * applied on local Loggers but can be inherited). Anyway, this method
	 * has passed the hurdles and needs to be queued.
	 */
	@Override
	public void publish(LogRecord record) {
		AbstractLogger l;
		
		synchronized (loggers) {
			
			//
			// Try to find the name in the cache. If not found, create a new one.
			//
			
			l = loggers.get(record.getLoggerName());
			if (l == null) {
				l = new AbstractLogger(LoggerDispatcher.classContext.getCallerBundle(), record.getLoggerName());
				loggers.put(record.getLoggerName(), l);
			}
			
			Level level = record.getLevel();

			// 
			// Translate the JUL to the OSGi log levels
			//
			
			if (level == Level.INFO) {
				if (l.isInfoEnabled())
					message(l, LogService.LOG_INFO, record);
				return;
			}
			if (level == Level.SEVERE) {
				if (l.isErrorEnabled())
					message(l, LogService.LOG_ERROR, record);
				return;
			}
			if (level == Level.WARNING) {
				if (l.isWarnEnabled())
					message(l, LogService.LOG_WARNING, record);
				return;
			}

			//
			// All other levels we assume are trace levels
			//
			
			if (l.isTraceEnabled())
				message(l, LoggerAdminImpl.LOG_TRACE, record);
		}
	}

	static Object[]	EMPTY	= new Object[0];

	/*
	 * convert what we have to an OSGi record
	 */
	private void message(AbstractLogger l, int level, LogRecord record) {
		Object[] parameters = record.getParameters();
		if (parameters == null)
			parameters = EMPTY;

		l.message(level, record.getMessage(), parameters);
	}

	/* Duh ...
	 */
	@Override
	public void flush() {}

	/*
	 * Wow, a life cycle method ... we just close all our loggers.
	 */
	@Override
	public void close() throws SecurityException {
		synchronized (loggers) {
			for (AbstractLogger l : loggers.values()) {
				l.close();
			}
			loggers.clear();
		}
	}

	/*
	 *  TODO Not sure if this is needed
	 */
	void update(Map<Pattern,Control> controls) {
	}

}
