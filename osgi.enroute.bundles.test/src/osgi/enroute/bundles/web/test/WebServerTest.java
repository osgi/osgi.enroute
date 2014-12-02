package osgi.enroute.bundles.web.test;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.JarResource;
import aQute.lib.io.IO;

public class WebServerTest extends TestCase {
	BundleContext bc = FrameworkUtil.getBundle(WebServerTest.class)
			.getBundleContext();

	public void testWebserver() throws BundleException, Exception {
		try (Builder b = new Builder()) {
			b.setBundleSymbolicName("web.tb1");
			b.setProperty("-includeresource",
					"static/tb1/foo.js;literal='foo.js\n',static/tb1/a.js;literal='a.js\n'");
			b.build();
			assertTrue(b.check());

			Bundle bundle = bc.installBundle("web.tb1",
					new JarResource(b.getJar()).openInputStream());
			try {
				bundle.start();
				URL u = new URL("http://localhost:8080/tb1/foo.js");

				String s = IO.collect(u.openStream());
				assertEquals("foo.js\n", s);

				
				HttpURLConnection con = (HttpURLConnection) u.openConnection();
				con.connect();
				assertEquals("application/javascript", con.getContentType());
				
				
				BundleContext c = bundle.getBundleContext();
				Hashtable<String,Object> p = new Hashtable<>();
				p.put("osgi.enroute.plugin.for", "foo");
				p.put("osgi.enroute.contributions", new String[] {"tb1/foo.js", "tb1/a.js"});
				c.registerService(Object.class, new Object(), p);
				
				URL u2 = new URL("http://localhost:8080/osgi.enroute.contributions/foo");
				String string = IO.collect(u2.openStream());
				
				System.out.println( string );
				assertTrue( string.contains("foo.js\n"));
				assertTrue( string.contains("\na.js\n"));
				
			} finally {
				bundle.uninstall();
			}
		}
	}
}
