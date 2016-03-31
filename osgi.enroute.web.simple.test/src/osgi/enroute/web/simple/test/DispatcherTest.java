package osgi.enroute.web.simple.test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.testing.DSTestWiring;
import aQute.lib.io.IO;
import aQute.libg.map.MAP;
import junit.framework.TestCase;
import osgi.enroute.servlet.api.ConditionalServlet;

public class DispatcherTest extends TestCase {

	BundleContext	context	= FrameworkUtil.getBundle(DispatcherTest.class).getBundleContext();
	DSTestWiring	ds		= new DSTestWiring();

	public void setUp() throws Exception {
		ds.setContext(context);
		ds.add(this);
		ds.wire();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testAccess() throws Exception {
		URL url = new URL("http://localhost:8080/");
		String s = IO.collect(url.openStream());
		System.out.println(s);
	}

	StringBuffer		buffer = new StringBuffer();
	
	public class TestSkipServlet implements ConditionalServlet {
		private String id;

		public TestSkipServlet(String id) {
			this.id = id;
		}

		@Override

		public boolean doConditionalService(HttpServletRequest rq, HttpServletResponse rsp) throws Exception {
			buffer.append(id).append(".");
			return false;
		}

	}
	
	public void testSkippingServlets() throws Exception {
		ServiceRegistration<ConditionalServlet> r10 = context.registerService(ConditionalServlet.class,
				new TestSkipServlet("10"), MAP.$(Constants.SERVICE_RANKING, 10).asHashtable());
		ServiceRegistration<ConditionalServlet> r20 = context.registerService(ConditionalServlet.class,
				new TestSkipServlet("20"), MAP.$(Constants.SERVICE_RANKING, 20).asHashtable());
		ServiceRegistration<ConditionalServlet> r30 = context.registerService(ConditionalServlet.class,
				new TestSkipServlet("30"), MAP.$(Constants.SERVICE_RANKING, 30).asHashtable());
		ServiceRegistration<ConditionalServlet> r40 = context.registerService(ConditionalServlet.class,
				new TestSkipServlet("40"), MAP.$(Constants.SERVICE_RANKING, 40).asHashtable());
		ServiceRegistration<ConditionalServlet> r50 = context.registerService(ConditionalServlet.class,
				new TestServlet("50"), MAP.$(Constants.SERVICE_RANKING, 50).asHashtable());

		try {
			assertContents( "50:/foo", "http://localhost:8080/foo");
			
			// Check calling order
			assertEquals("10.20.30.40.", buffer.toString());
			
			// Check if the normal resource read works
			assertContents( "FOO", "http://localhost:8080/foo/bar/test.txt");
		} finally {
			r10.unregister();
			r20.unregister();
			r30.unregister();
			r40.unregister();
			r50.unregister();
		}
	}

	private void assertContents(String content, String uri) throws IOException {
		URL url = new URL(uri);
		String s = IO.collect(url.openStream());
		assertEquals(content, s);
	}

	public class TestServlet implements ConditionalServlet {
		private String id;

		public TestServlet(String id) {
			this.id = id;
		}

		@Override

		public boolean doConditionalService(HttpServletRequest rq, HttpServletResponse rsp) throws Exception {
			String path = rq.getServletPath();
			if ( path != null && path.equals("/foo")) {
				rsp.getWriter().print(id + ":" + path);
				return true;
			}
			return false;
		}

	}

	public void testRankingServlets() throws Exception {
		ServiceRegistration<ConditionalServlet> r100 = context.registerService(ConditionalServlet.class,
				new TestServlet("100"), MAP.$(Constants.SERVICE_RANKING, 100).asHashtable());
		ServiceRegistration<ConditionalServlet> r50 = context.registerService(ConditionalServlet.class,
				new TestServlet("50"), MAP.$(Constants.SERVICE_RANKING, 50).asHashtable());

		try {
			URL url = new URL("http://localhost:8080/foo");
			String s = IO.collect(url.openStream());
			assertEquals("50:/foo", s);
		} finally {
			r100.unregister();
			r50.unregister();
		}
	}

	public class BadServlet implements ConditionalServlet {
		int n = 1;

		@Override
		public boolean doConditionalService(HttpServletRequest rq, HttpServletResponse rsp) throws Exception {
			if (n-- > 0) {
				rsp.getWriter().print(rq.getServletPath());
				return true;
			}
			else {
				n++;
				throw new IllegalArgumentException();
			}
		}

	}

	public void testBadServlet() throws Exception {
		ServiceRegistration<ConditionalServlet> blacklist = context.registerService(ConditionalServlet.class,
				new BadServlet(), MAP.$(Constants.SERVICE_RANKING, 100).asHashtable());

		try {
			URL url = new URL("http://localhost:8080/foo");
			String s = IO.collect(url.openStream());
			assertEquals("/foo", s);

			HttpURLConnection openConnection = (HttpURLConnection) url.openConnection();
			assertEquals(404, openConnection.getResponseCode());

			assertEquals("/foo", s);
		} finally {
			blacklist.unregister();
		}
	}

}
