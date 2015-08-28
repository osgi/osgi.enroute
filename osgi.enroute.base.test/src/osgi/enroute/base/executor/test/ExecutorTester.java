package osgi.enroute.base.executor.test;

import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import aQute.bnd.annotation.component.Reference;
import aQute.bnd.testing.DSTestWiring;
import junit.framework.TestCase;
import osgi.enroute.base.configurer.test.ConfigurerTest;

public class ExecutorTester extends TestCase {

	BundleContext		context	= FrameworkUtil.getBundle(ConfigurerTest.class).getBundleContext();
	DSTestWiring		ds		= new DSTestWiring();
	private Executor	executor;

	public void setUp() throws Exception {
		ds.setContext(context);
		ds.add(this);
		ds.wire();
	}

	public void testExecutor() throws Exception {
		final Semaphore s = new Semaphore(0);
		executor.execute(new Runnable() {

			@Override
			public void run() {
				s.release();
			}
		});
		assertTrue(s.tryAcquire(1, 100, TimeUnit.SECONDS));
	}

	@Reference
	void setExecutor(Executor executor) {
		this.executor = executor;

	}
}
