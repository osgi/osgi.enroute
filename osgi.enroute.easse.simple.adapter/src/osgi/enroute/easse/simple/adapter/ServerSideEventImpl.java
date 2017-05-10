package osgi.enroute.easse.simple.adapter;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.log.LogService;

import aQute.lib.json.JSONCodec;
import osgi.enroute.http.capabilities.RequireHttpImplementation;

/**
 * This component provides a servlet that allows javascript clients to see the
 * Event Admin events. It blocks the request thread until it gets killed.
 * <p>
 * The request path is treated as an EventAdmin topic filter, it is the
 * EVENT_TOPIC service property on an Event Handler service.
 * <p>
 * If the client registers with an {@code instance=<id>} then the request thread
 * can be killed from another request by specifying {@code abort=<id>}. This was
 * necessary because automatic closing did not work very well.
 * <p>
 * For IE-9, this article was very helpfull: <a href=
 * "http://blogs.msdn.com/b/ieinternals/archive/2010/04/06/comet-streaming-in-internet-explorer-with-xmlhttprequest-and-xdomainrequest.aspx"
 * >Comet Streaming in Explorer with XMLHttpRequest and XDomainRequest</a>
 * 
 */
@RequireHttpImplementation
@Component(name = "osgi.eventadmin.sse", property = HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN+"=/sse/1/*", service = Servlet.class, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class ServerSideEventImpl extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static JSONCodec codec = new JSONCodec();
	private static byte[] prelude;
	private static Random random = new SecureRandom();
	final Map<String, Thread> threads = new ConcurrentHashMap<String, Thread>();
	BundleContext context;
	
	@Reference
	LogService log;

	@Activate
	void activate(BundleContext context) {
		this.context = context;
	}

	@Deactivate
	void deactivate() {
		for (Thread out : threads.values()) {
			out.interrupt();
		}
	}

	@Override
	public void doGet(HttpServletRequest rq, HttpServletResponse rsp)
			throws IOException {
		//
		// First some house cleaning. The caller can abort
		// a previous connection. The request then passes abort=instanceId.
		//

		String instanceId = rq.getParameter("abort");
		if (instanceId != null) {
			kill(instanceId);
			rsp.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		//
		// Now we need to get an instance id, if it does not
		// exist than we create a random one.
		//

		instanceId = rq.getParameter("instance");
		if (instanceId == null)
			instanceId = random.nextLong() + "";
		else
			kill(instanceId);

		String path = rq.getPathInfo();
		if (path == null || path.isEmpty()) {
			rsp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect path "
					+ path);
			return;
		}

		final String topic = path.substring(1); // skip leading /

		//
		// Access-Control-Allow-Origin header, seems crucial for IE-9 although
		// I do not know why??
		//

		rsp.setHeader("Access-Control-Allow-Origin", "*");
		rsp.setContentType("text/event-stream;charset=utf-8");

		final Thread thread = Thread.currentThread();
		OutputStream out = rsp.getOutputStream();

		//
		// We need to clean up old connections from this process
		// The caller gives us an instance id, which it calculates per
		// JS/page. If this same page reconnects, we kill the old
		// connection
		//

		threads.put(instanceId, thread);

		final PrintStream pout = new PrintStream(out);
		final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>(
				20);
		final AtomicReference<Closeable> ref = new AtomicReference<Closeable>(
				out);
		ServiceRegistration<?> registration = register(topic, eventQueue,
				instanceId, ref, thread);

		try {

			//
			// The 'programmers' at M$ implemented streaming but forgot about a
			// lower layer's buffering. So to make streaming work, we must send
			// a 2k prelude
			//

			String userAgent = rq.getHeader("User-Agent");
			if (userAgent != null && userAgent.contains("MSIE 9.")) {
				out.write(getPrelude());
				out.flush();
			}

			pout.printf(": welcome\n\n");
			pout.flush();

			while (true) {
				Event event = eventQueue.poll(2, TimeUnit.SECONDS);
				if (event == null) {
					pout.print(":\n\n");
				} else {
					Map<String, Object> props = new HashMap<String, Object>();
					for (String name : event.getPropertyNames()) {
						props.put(name, event.getProperty(name));
					}
					pout.printf("type: org.osgi.service.eventadmin;topic=%s\n",
							topic);

					String json = codec.enc().put(props).toString();
					pout.printf("data: %s\n\n", json);
				}
				pout.flush();
			}

		} catch (InterruptedException ie) {
			rsp.setStatus(HttpServletResponse.SC_OK);
		} catch (Exception e) {
			log.log(LogService.LOG_INFO, "Quiting " + topic, e);
			// time to close ...
		} finally {
			threads.remove(instanceId);
			registration.unregister();
			if (ref.getAndSet(null) == null) {
				
				//
				// A little grace period since we could be interrupted
				// and do not want to kill the next request

				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// OK, we we're hoping for it
				}
			}
			out.close();
		}
	}

	/**
	 * @param topic
	 * @param eventQueue
	 * @param instanceId
	 * @param out
	 * @return
	 */
	private ServiceRegistration<?> register(final String topic,
			final BlockingQueue<Event> eventQueue, String instanceId,
			final AtomicReference<Closeable> out, final Thread thread) {
		Hashtable<String, String> p = new Hashtable<String, String>();
		p.put(EventConstants.EVENT_TOPIC, topic);
		p.put("instance.id", instanceId);
		ServiceRegistration<?> registration = context.registerService(
				EventHandler.class.getName(), new EventHandler() {

					@Override
					public synchronized void handleEvent(Event event) {

						if (eventQueue.offer(event))
							return;

						//
						// Our queue is filling up, this is likely caused by
						// a dead SSE thread (browser closed without warning
						// us. So we kill it
						//

						Closeable o = out.getAndSet(null);
						if (o == null)
							//
							// Already killed
							//
							return;

						log.log(LogService.LOG_WARNING,
								"Killing orphaned GUI thread beause queue is full");

						//
						// First interrupt it so we kill it nicely
						//

						try {
							thread.interrupt();

							//
							// Then the hammer to kill for real
							//

							o.close();

						} catch (IOException e) {
						}

					}
				}, p);
		return registration;
	}

	/**
	 * Kill a running instance
	 * 
	 * @param instanceId
	 */
	private void kill(String instanceId) {
		Thread previous = threads.get(instanceId);
		if (previous != null) {
			previous.interrupt();
		}
	}

	/**
	 * Get the MSIE-9 prelude. There is an puny chance this method would
	 * initialize multiple times, but that is ok,.
	 */
	private static byte[] getPrelude() {
		if (prelude == null) {
			prelude = new byte[2048];
			prelude[0] = ':';
			for (int i = 1; i < prelude.length - 1; i++)
				prelude[i] = ' ';
			prelude[prelude.length - 1] = '\n';
		}
		return prelude;
	}

	@Reference
	void setLog(LogService log) {
		this.log = log;
	}

}
