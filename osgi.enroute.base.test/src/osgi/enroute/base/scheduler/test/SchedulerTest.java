package osgi.enroute.base.scheduler.test;

import java.time.Instant;
import java.util.Hashtable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

import osgi.enroute.base.configurer.test.ConfigurerTest;
import osgi.enroute.scheduler.api.CronJob;
import osgi.enroute.scheduler.api.Scheduler;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.testing.DSTestWiring;

@SuppressWarnings("rawtypes")
public class SchedulerTest extends TestCase {
	BundleContext context = FrameworkUtil.getBundle(ConfigurerTest.class)
			.getBundleContext();
	DSTestWiring ds = new DSTestWiring();
	private Scheduler scheduler;

	public void setUp() throws Exception {
		ds.setContext(context);
		ds.add(this);
		ds.wire();
	}

	static class Chk implements Success<Instant, Integer> {
		long start = System.currentTimeMillis();
		Semaphore semaphore = new Semaphore(0);

		@Override
		public Promise<Integer> call(Promise<Instant> resolved)
				throws Exception {
			semaphore.release();
			return null;
		}

		public void assertBetween(int atLeast, int atMost)
				throws InterruptedException {
			assertTrue("Expected at most " + atMost + " ms",
					semaphore.tryAcquire(atMost, TimeUnit.MILLISECONDS));

			assertTrue("Expected at least " + atLeast + " ms",
					System.currentTimeMillis() - atLeast >= 0);

		}

	};

	public void testTimer() throws Exception {
		Chk c = new Chk();
		scheduler.after(100).then(c);
		c.assertBetween(100, 200);
	}

	interface Data {
		int foo();
	}

	public static class Job implements CronJob<Data> {
		volatile int foo;
		volatile int incr = 0;
		
		@Override
		public void run(Data data) throws Exception {
			foo = data.foo();
			incr++;
		}
	}

	public void testReboot() throws Exception {
		Job job = new Job();

		ServiceRegistration<CronJob> reg = context.registerService(
				CronJob.class, job, new Hashtable<String, Object>() {
					private static final long serialVersionUID = 1L;

					{
						put(CronJob.CRON, "foo=10\n" + "@reboot");
					}
				});

		Thread.sleep(100);
		assertEquals(10, job.foo);
		reg.unregister();
	}

	public void testEveryOtherSecond() throws Exception {
		Job job = new Job();

		ServiceRegistration<CronJob> reg = context.registerService(
				CronJob.class, job, new Hashtable<String, Object>() {
					private static final long serialVersionUID = 1L;

					{
						put(CronJob.CRON, "foo=12\n" + "0/2 * * * * ?");
					}
				});

		Thread.sleep(11000);
		assertEquals(12, job.foo);
		assertTrue( job.incr >= 5);
		assertTrue( job.incr <= 6);
		reg.unregister();
	}

	@Reference
	void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

}
