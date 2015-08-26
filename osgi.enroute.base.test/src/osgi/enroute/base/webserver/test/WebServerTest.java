package osgi.enroute.base.webserver.test;

import java.io.File;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.http.HttpService;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import aQute.bnd.annotation.component.Reference;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.testing.DSTestWiring;
import junit.framework.TestCase;
import osgi.enroute.base.configurer.test.ConfigurerTest;
import osgi.enroute.webserver.capabilities.RequireWebServerExtender;

@RequireWebServerExtender
public class WebServerTest extends TestCase {
	private static final String HTML_BODY_TEST_BODY_HTML = "<html><body>test</body></html>";
	BundleContext context = FrameworkUtil.getBundle(ConfigurerTest.class)
			.getBundleContext();
	DSTestWiring ds = new DSTestWiring();

	public void setUp() throws Exception {
		ds.setContext(context);
		ds.add(this);
		ds.wire();
	}

	public void testWebResource() throws Exception {
		try (Builder b = new Builder();) {
			b.setProperties(new File("resources/webresources.bnd"));
			Jar jar = b.build();
			jar.getManifest().write(System.out);
			Bundle bundle = context.installBundle("test",
					new JarResource(jar).openInputStream());
			try {
				bundle.start();

				final WebClient webClient = new WebClient(
						BrowserVersion.FIREFOX_24);
				webClient.getOptions().setTimeout(0);
				final HtmlPage page = webClient
						.getPage("http://localhost:8080/test.html");

				//
				// Test if we downloaded our own script in web/test.js
				//

				ScriptResult result = page.executeJavaScript("foo");
				assertEquals("Yes, we can", result.getJavaScriptResult());

			} finally {
				bundle.uninstall();
			}
		}
	}

	public void testSimple() throws BundleException, Exception {
		Jar jar = new Jar("test");
		jar.putResource("static/test.html", new EmbeddedResource(
				HTML_BODY_TEST_BODY_HTML.getBytes(), 0));
		Bundle b = context.installBundle("test",
				new JarResource(jar).openInputStream());
		try {
			b.start();

			final WebClient webClient = new WebClient();
			final HtmlPage page = webClient
					.getPage("http://localhost:8080/test.html");
			assertEquals("test", page.asText());
		} finally {
			b.uninstall();
		}
	}

	// sync so we do not start before the http service is started
	@Reference
	void setHttp(HttpService s) {
	}
}
