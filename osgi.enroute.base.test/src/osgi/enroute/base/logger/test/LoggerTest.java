package osgi.enroute.base.logger.test;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Reference;
import aQute.bnd.testing.DSTestWiring;
import junit.framework.TestCase;
import osgi.enroute.base.configurer.test.ConfigurerTest;
import osgi.enroute.logger.api.Level;
import osgi.enroute.logger.api.LoggerAdmin;
import osgi.enroute.logger.api.LoggerAdmin.Control;
import osgi.enroute.logger.api.LoggerAdmin.Info;
import osgi.enroute.logger.api.LoggerAdmin.Settings;

public class LoggerTest extends TestCase {
	BundleContext	context	= FrameworkUtil.getBundle(ConfigurerTest.class).getBundleContext();
	DSTestWiring	ds		= new DSTestWiring();
	Logger			logger;
	LoggerAdmin		admin;
	BlockingQueue<LogEntry>	entries	= new LinkedBlockingQueue<LogEntry>();

	public void setUp() throws Exception {
		ds.setContext(context);
		ds.add(this);
		ds.wire();
	}

	public void testLogger() throws Exception {
		assertNotNull(logger);
		assertNotNull(admin);
		List<Info> list = admin.list(context.getBundle().getSymbolicName());
		assertEquals(0, list.size());
		
		logger.info("[silent]info"); // register
		list = admin.list(context.getBundle().getSymbolicName());
		assertEquals(1, list.size());
		
		Info info = list.get(0);
		assertEquals(context.getBundle().getBundleId(), info.bundleId);

		assertTrue(logger.isErrorEnabled());
		assertTrue(logger.isWarnEnabled());
		assertFalse(logger.isInfoEnabled());
		assertFalse(logger.isDebugEnabled());
		assertFalse(logger.isTraceEnabled());

		Settings settings = admin.getSettings();
		assertTrue(settings.controls.isEmpty());

		Control c = new Control();
		c.level = Level.INFO;
		c.pattern = "*";
		settings.controls.add(c);
		admin.setSettings(settings);

		assertTrue(logger.isErrorEnabled());
		assertTrue(logger.isWarnEnabled());
		assertTrue(logger.isInfoEnabled());
		assertFalse(logger.isDebugEnabled());
		assertFalse(logger.isTraceEnabled());

		assertEquals(1, entries.size());

		logger.error("[silent]error");
		assertNotNull( entries.poll(100000, TimeUnit.MILLISECONDS));
		
		System.out.println("wait");
	}

	public void testStaticLogger() throws Exception {
		Logger logger = LoggerFactory.getLogger(getClass());
		assertNotNull(logger);
		String qpat = getClass().getName();

		List<Info> list = admin.list(qpat);
		assertEquals(0, list.size());

		logger.info("[silent]info"); // register
		list = admin.list(qpat);		
		
		Info info = list.get(0);
		assertEquals(context.getBundle().getBundleId(), info.bundleId);

		assertTrue(logger.isErrorEnabled());
		assertTrue(logger.isWarnEnabled());
		assertFalse(logger.isInfoEnabled());
		assertFalse(logger.isDebugEnabled());
		assertFalse(logger.isTraceEnabled());

		Settings settings = admin.getSettings();
		assertTrue(settings.controls.isEmpty());

		Control c = new Control();
		c.level = Level.INFO;
		c.pattern = qpat;
		settings.controls.add(c);
		admin.setSettings(settings);

		assertTrue(logger.isErrorEnabled());
		assertTrue(logger.isWarnEnabled());
		assertTrue(logger.isInfoEnabled());
		assertFalse(logger.isDebugEnabled());
		assertFalse(logger.isTraceEnabled());
	}

	@Reference
	void setLogger(Logger logger) {
		this.logger = logger;
	}

	@Reference
	void setLogReaderService(LogReaderService lrs) {
		entries.clear();
		lrs.addLogListener(new LogListener() {

			@Override
			public void logged(LogEntry entry) {
				if (entry.getBundle() == context.getBundle()) {
					entries.add(entry);
				}
			}
		});
	}

	@Reference
	void setLoggerAdmin(LoggerAdmin logger) throws Exception {
		this.admin = logger;
		Settings settings = this.admin.getSettings();
		settings.controls.clear();
		this.admin.setSettings(settings);
	}
}
