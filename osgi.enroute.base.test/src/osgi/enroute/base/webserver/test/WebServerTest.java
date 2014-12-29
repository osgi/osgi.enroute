package osgi.enroute.base.webserver.test;

import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.http.HttpService;

import osgi.enroute.base.configurer.test.ConfigurerTest;
import osgi.enroute.capabilities.WebServerExtender;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.testing.DSTestWiring;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

@WebServerExtender.Require
public class WebServerTest extends TestCase {
	private static final String HTML_BODY_TEST_BODY_HTML = "<html><body>test</body></html>";
	BundleContext	context	= FrameworkUtil.getBundle(ConfigurerTest.class).getBundleContext();
	DSTestWiring	ds		= new DSTestWiring();

	public void setUp() throws Exception {
		ds.setContext(context);
		ds.add(this);
		ds.wire();
	}


	public void testSimple() throws BundleException, Exception {
		Jar jar = new Jar("test");
		jar.putResource("static/test.html", new EmbeddedResource(HTML_BODY_TEST_BODY_HTML.getBytes(), 0));
		Bundle b = context.installBundle("test", new JarResource(jar).openInputStream());
		b.start();
		
	    final WebClient webClient = new WebClient();
	    final HtmlPage page = webClient.getPage("http://localhost:8080/test.html");
	    assertEquals("test", page.asText());
	}


	@Reference
	void setHttp(HttpService s) {
	}
}
