package osgi.enroute.scheduler.simple.adapter;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

public class SimpleImplTest extends TestCase {

	private SchedulerSimpleImpl ssi;

	public void setUp() throws Exception {
		ssi = new SchedulerSimpleImpl();
		ssi.activate(null);
	}

	public void tearDown() throws Exception {
		ssi.deactivate();
	}

	public void testSimple() throws Exception {
		Instant start = Instant.now();
		AtomicReference<Instant> then = new AtomicReference<>();
		Promise<Integer> p = ssi.delay(() -> {
			return 5;
		}, 500);
		p.onResolve(() -> then.set(Instant.now()));

		Thread.sleep(800);
		assertNotNull(then.get());

		long diff = then.get().toEpochMilli() - start.toEpochMilli();
		assertTrue(diff > 450 && diff < 600);
	}

	public void testValueReturn() throws Exception {
		Instant start = Instant.now();
		AtomicReference<Instant> then = new AtomicReference<>();
		Promise<Integer> p = ssi.delay(10, 500);
		p.onResolve(() -> then.set(Instant.now()));

		Thread.sleep(440);

		assertFalse(p.isDone());

		Thread.sleep(800);
		assertTrue(p.isDone());
		assertEquals(10, (int) p.getValue());
		assertNotNull(then.get());

		long diff = then.get().toEpochMilli() - start.toEpochMilli();
		assertTrue(diff > 450 && diff < 600);
	}

	public void testBeforeWithTimeout() throws Exception {
		Deferred<Integer> deferred = new Deferred<>();

		Promise<Integer> p = ssi.before(deferred.getPromise(), 300);

		Thread.sleep(200);
		assertFalse(p.isDone());
		
		Thread.sleep(400);
		assertTrue(p.isDone());
		assertNotNull( p.getFailure());
		assertTrue( p.getFailure() instanceof osgi.enroute.scheduler.api.TimeoutException);
	}

	public void testBeforeOnTime() throws Exception {
		Deferred<Integer> deferred = new Deferred<>();

		Promise<Integer> p = ssi.before(deferred.getPromise(), 300);

		Thread.sleep(200);
		assertFalse(p.isDone());
		deferred.resolve(10);
		
		Thread.sleep(400);
		assertTrue(p.isDone());
		assertNull( p.getFailure());
		assertEquals( 10, (int) p.getValue());
	}
	
	public void testAfter() throws Exception {
		Deferred<Integer> deferred = new Deferred<>();
		Promise<Integer> p = ssi.after(deferred.getPromise(), 100);
		
		deferred.resolve(10);
		assertFalse(p.isDone());
		Thread.sleep(50);
		assertFalse(p.isDone());
		
		Thread.sleep(150);
		assertTrue(p.isDone());
		assertEquals( 10, (int) p.getValue());
	}
	public void testDelay() throws Exception {
		Deferred<Integer> deferred = new Deferred<>();
		
		Promise<Integer> p = deferred.getPromise().then(ssi.delay(100));
		
		deferred.resolve(10);
		assertFalse(p.isDone());
		Thread.sleep(50);
		assertFalse(p.isDone());
		
		Thread.sleep(150);
		assertTrue(p.isDone());
		assertEquals( 10, (int) p.getValue());
	}

}
