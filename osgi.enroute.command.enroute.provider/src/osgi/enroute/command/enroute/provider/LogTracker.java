package osgi.enroute.command.enroute.provider;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This class provides a listener for the log service so that clients can decide
 * how big the buffer is. Valentin Valchev gave some good hints:
 * <p>
 * 1. You have a bundle with native code - there is an exception class declared
 * in that bundle - the exception is thrown somewhere and logged - if the log
 * entry, with the exception above is inside the log queue, the bundle update
 * will fail the reason is that the class loader of the first version of the
 * bundle cannot be discarded, so the native library is not unloaded. When the
 * new version is installed, and tries to load the native library it will fails,
 * because the library is already loaded. ! the solution is to use the method
 * mentioned by LogReaderService.getLog() - to wrap the exception so you don't
 * keep references to the original exception
 * </p>
 * 2. If you are using IBM J9/VAME virtual machine : - you may throw and log
 * IOException as example - in that case your bundle becomes part of the stack
 * trace - your bundle is updated or uninstalled - another bundle uses the log
 * reader to get the exceptions - when that bundle gets the IOException and
 * tries to print the stack trace the virtual machine dies ! the solution is
 * same as above
 * <p>
 * To prevent the aforementioned problems we print the stack trace and store it
 * in the log as a string.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class LogTracker extends ServiceTracker implements Runnable {
	/**
	 * Max number of entries in the buffer
	 */
	final int SIZE = 200;

	/*
	 * Used to extract the first line of the stack trace
	 */
	private static Pattern FIRST_LINE_P = Pattern.compile("^([^\n\r]{0,80})");

	final BundleContext context;
	final ConcurrentLinkedDeque<LE> list = new ConcurrentLinkedDeque<LE>();
	final LinkedBlockingQueue<LogEntry> queue = new LinkedBlockingQueue<LogEntry>(100);
	final Thread thread = new Thread(this, "Apache Felix::Log Listener");

	private List<PrintStream> consoles = new CopyOnWriteArrayList<>();

	/**
	 * Log Level - Aligned with the OSGi Log levels (i.e. ordinal == OSGi)
	 */
	public enum Level {
		unknown, error, warning, info, debug
	}

	/**
	 * Print style
	 */
	public enum Style {
		classic, abbr;
	}

	/**
	 * DTO to hold the serialized information about a log entry
	 */
	static class LE {
		int id;
		String message;
		String exception;
		Level level;
		long serviceId = -1;
		String service;
		long time;
		String bundle;
		long bundleId;

	}

	/**
	 * Constructor
	 * 
	 */
	public LogTracker(BundleContext context) {
		super(context, LogReaderService.class.getName(), null);
		this.context = context;
		super.open();
		thread.start();
	}

	/**
	 * Get rid of our thread
	 */

	public void close() {
		thread.interrupt();
	}

	/**
	 * Tracks Log Services. Will merge all records if there are several.
	 */
	public Object addingService(ServiceReference ref) {
		LogReaderService lrs = (LogReaderService) super.addingService(ref);
		lrs.addLogListener(new LogListener() {

			public void logged(LogEntry entry) {
				queue.offer(entry);
			}

		});
		Enumeration<LogEntry> e = lrs.getLog();
		while (e.hasMoreElements())
			queue.offer(e.nextElement());

		return lrs;
	}

	/**
	 * Background thread to convert Log Entry records to a form that is not
	 * holding any references.
	 */
	public void run() {

		int currentId = 1000;

		try {
			while (!thread.isInterrupted()) {

				//
				// We block until we get an entry or are interrupted
				//

				LogEntry entry = queue.take();

				
				
				
				//
				// Get the data in a non-reference form
				//

				LE le = new LE();
				le.id = currentId++;
				le.message = entry.getMessage();
				le.level = getLevel(entry.getLevel());
				le.time = entry.getTime();

				Bundle b = entry.getBundle();
				if (b != null) {
					le.bundleId = entry.getBundle().getBundleId();
					le.bundle = entry.getBundle().getSymbolicName();
				}
				if (entry.getServiceReference() != null) {
					le.serviceId = (Long) entry.getServiceReference().getProperty(Constants.SERVICE_ID);
					le.service = entry.getServiceReference().toString();
				}

				if (entry.getException() != null) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					entry.getException().printStackTrace(pw);
					pw.close();
					le.exception = sw.toString();
				}

				for ( PrintStream console : consoles) try {
					Formatter f = new Formatter();
					abbr(f,le,true);
					console.println( f.toString());
				} catch( Exception  e){
					// ignore
				}
				list.add(le);

				//
				// Shrink the list if it gets too long
				//

				if (list.size() > SIZE)
					list.removeLast();

			}
		} catch (InterruptedException ie) {
			// ok, we're done here
		}
	}

	/*
	 * Convert an integer level to the enum
	 */
	private Level getLevel(int level) {
		switch (level) {
		case LogService.LOG_ERROR:
			return Level.error;
		case LogService.LOG_WARNING:
			return Level.warning;
		case LogService.LOG_INFO:
			return Level.info;
		default:
			return Level.debug;
		}
	}

	/*
	 * Select the entries from the log.
	 * 
	 * @param maxEntries Max number of matching entries to fetch
	 * 
	 * @param skip Number of entries to skip
	 * 
	 * @param logLevel The minimum level
	 * 
	 * @param reverse Return a reversed list or not
	 * 
	 * @return a filtered list
	 */
	public List<LE> select(int maxEntries, int skip, Level logLevel, boolean reverse) {

		List<LE> result = new ArrayList<LE>();

		for (LE entry : list) {

			if (entry.level.compareTo(logLevel) > 0)
				continue;

			if (skip-- > 0)
				continue;

			if (maxEntries-- <= 0)
				break;

			result.add(entry);
		}

		if (reverse)
			Collections.reverse(result);

		return result;
	}

	/**
	 * Entry method to get the log. Lots of options
	 * 
	 * @param maxEntries
	 *            Max number of matching entries to fetch
	 * @param skip
	 *            Number of entries to skip
	 * @param logLevel
	 *            The minimum level
	 * @param reverse
	 *            Return a reversed list or not
	 * @param noExceptions
	 *            Do not print exceptions
	 * @param style
	 *            Different print styles
	 * @return a formatted list
	 */
	public List<String> log(int maxEntries, int skip, Level logLevel, boolean reverse, boolean noExceptions,
			Style style) {

		List<LE> selected = select(maxEntries, skip, logLevel, reverse);
		List<String> result = new ArrayList<String>(selected.size());

		StringBuilder sb = new StringBuilder();
		Formatter f = new Formatter(sb);
		try {
			for (LE entry : selected) {

				//
				// Clear the buffer, we reuse one buffer for efficiency
				//

				sb.setLength(0);

				switch (style) {
				case abbr:
					abbr(f, entry, noExceptions);
					break;

				default:
					classic(f, entry, noExceptions);
					break;
				}
				f.flush();
				result.add(sb.toString());
			}
		} finally {
			f.close();
		}
		return result;
	}

	/*
	 * Classic formatting
	 */
	private void classic(Formatter f, LE entry, boolean noExceptions) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
		f.format("%s %s - Bundle: %s", sdf.format(new Date(entry.time)), entry.level, entry.bundle);

		if (entry.serviceId != -1) {
			f.format(" - %s", entry.service);
		}

		f.format(" - %s", entry.message);

		if (entry.exception != null && !noExceptions) {
			f.format(" - %s", entry.exception);
		}
	}

	/*
	 * Abbreviated formatting
	 */
	private void abbr(Formatter f, LE entry, boolean noExceptions) {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String firstLine = "";
		if (entry.exception != null) {
			Matcher matcher = FIRST_LINE_P.matcher(entry.exception);
			if (matcher.find())
				firstLine = matcher.group(1);
		}

		f.format("%4d %s %-6s %-40s %s %s", entry.id, sdf.format(new Date(entry.time)), entry.level, entry.bundle,
				entry.message, firstLine);
	}

	public void addConsole(PrintStream console) {
		consoles .add(console);
		
	}

	public void removeConsole(PrintStream console) {
		consoles .add(console);
		
	}
}
