package osgi.enroute.base.timer.test;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import osgi.enroute.base.configurer.test.ConfigurerTest;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.testing.DSTestWiring;

public class TimerTester extends TestCase {

	BundleContext	context	= FrameworkUtil.getBundle(ConfigurerTest.class).getBundleContext();
	DSTestWiring	ds		= new DSTestWiring();
	private Timer	timer;

	public void setUp() throws Exception {
		ds.setContext(context);
		ds.add(this);
		ds.wire();
	}

	public void testTimer() throws Exception {
		final Semaphore s = new Semaphore(0);
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				s.release();
			}
		}, 100);
		assertTrue(s.tryAcquire(1, 100, TimeUnit.SECONDS));
	}

	@Reference
	void setTimer(Timer timer) {
		this.timer = timer;

	}
}
