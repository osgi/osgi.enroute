package osgi.enroute.base.scheduler.test;

import java.time.Instant;
import java.util.Hashtable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;

import aQute.bnd.annotation.component.Reference;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.testing.DSTestWiring;
import junit.framework.TestCase;
import osgi.enroute.base.configurer.test.ConfigurerTest;
import osgi.enroute.scheduler.api.CronJob;
import osgi.enroute.scheduler.api.Scheduler;

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

	public void testBundleCleanup() throws Exception {
		Builder b = new Builder();
		b.setBundleSymbolicName("test.1");
		b.setProperty("-resourceonly", "true");
		Jar j = b.build();
		Bundle btest1 = context.installBundle("test.1", new JarResource(j).openInputStream());
		btest1.start();
		
		BundleContext bc = btest1.getBundleContext();
		
		ServiceReference<Scheduler> ref = bc.getServiceReference(Scheduler.class);
		assertNotNull(ref);
		
		Scheduler s = bc.getService(ref);
		assertNotNull(s);
		
		assertNotSame(scheduler, s);
		
		Semaphore semaphore = new Semaphore(0);
		
		s.after(() -> semaphore.release(), 100);
		
		assertBetween( semaphore, 90, 110);
		
		s.after(() -> semaphore.release(), 100);
		btest1.stop();
		
		if (semaphore.tryAcquire(1, 200, TimeUnit.MILLISECONDS))
			fail("Stopping the bundle did not clean up");
		
		b.close();
	}
	
	
	
	private void assertBetween(Semaphore s, int min, int max) throws InterruptedException {
		long now = System.currentTimeMillis();
		if (!s.tryAcquire(1, max, TimeUnit.MILLISECONDS))
				fail("Took more than " + max + "ms to get semaphore");

		long diff = System.currentTimeMillis() - now;
		System.out.println("time it took " + diff );
		
		assertTrue("Took less than " + min + "ms to get permit", diff >= min);
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
