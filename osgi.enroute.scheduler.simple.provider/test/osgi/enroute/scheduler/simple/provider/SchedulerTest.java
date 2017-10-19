package osgi.enroute.scheduler.simple.provider;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

import org.osgi.util.promise.Deferred;

import osgi.enroute.scheduler.api.CancelException;
import osgi.enroute.scheduler.api.CancellablePromise;
import osgi.enroute.scheduler.api.CronJob;
import osgi.enroute.scheduler.api.TimeoutException;

public class SchedulerTest extends TestCase {
	InternalSchedulerImpl si = new InternalSchedulerImpl();

	
	public static class TestData {
		
	}
	public void testCronJobType() {
//		CronJob<TestData> cj = (data) -> {};
		CronJob<TestData> cj = new CronJob<SchedulerTest.TestData>() {

			@Override
			public void run(TestData data) throws Exception {
				// TODO Auto-generated method stub

			}
		};
		assertEquals(TestData.class, si.getType(cj));
	}




	@Override
	public void tearDown() {
		si.deactivate();
	}

	interface Foo {
		String foo();
	}

	public void testCronReboot() throws Exception {
		long now = System.currentTimeMillis();
		Semaphore s = new Semaphore(0);
		si.schedule(Foo.class, (foo) -> {
			s.release();
		}, "@reboot");
		s.acquire(1);

		long diff = (System.currentTimeMillis() - now);
		assertTrue(diff < 100);
	}

	public void testCron2() throws Exception {
		long now = System.currentTimeMillis();
		AtomicReference<String> ref = new AtomicReference<>();

		Semaphore s = new Semaphore(0);
		si.schedule(Foo.class, (foo) -> {
			s.release();
			ref.set(foo.foo());
		}, "#\n" //
				+ "\n" //
				+ " foo = bar \n" //
				+ "# bla bla foo=foo\n" //
				+ "0/2 * * * * *");
		s.acquire(2);

		long diff = (System.currentTimeMillis() - now + 500) / 1000;
		assertTrue(diff >= 3 && diff <= 4);
		assertEquals("bar", ref.get());
	}

	public void testCancellableWithTimeout() throws InterruptedException,
			InvocationTargetException {
		Deferred<Integer> d = new Deferred<>();
		CancellablePromise<Integer> before = si.before(d.getPromise(), 100);
		before.cancel();
		assertEquals(CancelException.SINGLETON, before.getFailure());
	}

	public void testResolveWithTimeout() throws InterruptedException,
			InvocationTargetException {
		Deferred<Integer> d = new Deferred<>();
		CancellablePromise<Integer> before = si.before(d.getPromise(), 100);
		d.resolve(3);
		assertTrue(before.isDone());
		assertEquals(Integer.valueOf(3), before.getValue());
	}

	public void testFailureWithTimeout() throws InterruptedException,
			InvocationTargetException {
		Deferred<Integer> d = new Deferred<>();
		CancellablePromise<Integer> before = si.before(d.getPromise(), 100);
		Exception e = new Exception();
		d.fail(e);
		assertTrue(before.isDone());
		assertEquals(e, before.getFailure());
	}

	public void testTimeout() throws InterruptedException,
			InvocationTargetException {
		Deferred<Integer> d = new Deferred<>();
		CancellablePromise<Integer> before = si.before(d.getPromise(), 100);
		Thread.sleep(200);
		assertTrue(before.isDone());
		assertEquals(TimeoutException.SINGLETON, before.getFailure());
	}

	public void testNegative() throws InterruptedException {
//		Semaphore s = new Semaphore(0);
//		si.after(() -> {
//			s.release(1);
//			return null;
//		}, -100);
//		Thread.sleep(2);
//		assertEquals(1, s.availablePermits());
	}

	public void testCron() throws Exception {
		long now = System.currentTimeMillis();

		Semaphore s = new Semaphore(0);
		si.schedule(() -> s.release(), "0/2 * * * * *");
		s.acquire(3);

		long diff = (System.currentTimeMillis() - now + 500) / 1000;
		assertTrue(diff >= 5 && diff <= 6);
	}

	public void testSchedule() throws InterruptedException, IOException {
		// Don't run on AppVeyor, probably too slow
		//
		if ( File.pathSeparatorChar == '\\')
			return;
		
		long now = System.currentTimeMillis();

		Semaphore s = new Semaphore(0);
		try (Closeable c = si.schedule(() -> s.release(), 100, 200, 300, 400)) {

			s.acquire(3);
			long diff = System.currentTimeMillis() - now;
			assertEquals(6, (diff + 50) / 100);

			int n = s.availablePermits();
			Thread.sleep(3000);
			assertEquals(n + 7, s.availablePermits());
			c.close();
			n = s.availablePermits();
			Thread.sleep(3000);
			assertEquals(n, s.availablePermits());
		}
	}

	public void testSimple() throws InterruptedException {
		long now = System.currentTimeMillis();

		Semaphore s = new Semaphore(0);

		si.after(10).then((p) -> {
			s.release(1);
			return null;
		});

		s.acquire();

		assertTrue(System.currentTimeMillis() - now > 9);

		si.deactivate();
	}
}
