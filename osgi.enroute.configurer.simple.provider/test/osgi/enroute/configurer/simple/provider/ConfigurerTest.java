package osgi.enroute.configurer.simple.provider;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;

import aQute.bnd.junit.JUnitFramework;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.URLResource;
import aQute.lib.io.IO;

/*
 * 
 * 
 * 
 */

public class ConfigurerTest {

	private static final long DELAY = 1000;
	static JUnitFramework juf;

	@BeforeClass
	public static void beforClasse() throws Exception {
		juf = new JUnitFramework();
		juf.addBundle("org.apache.felix.configadmin");
		juf.addBundle("org.apache.felix.log");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		juf.close();
	}

	private ConfigurationAdmin	cm;
	private Configurer			cfg;

	@Before
	public void before() throws Exception {
		System.setProperty("enRoute.configurer.extra", "[\n" +
				"   { \n" +
				"      \"service.pid\":							\"system\",\n" +
				"      \"data\":								\"data\"\n" +
				"   }\n" +
				"]\n" +
				"");
		cfg = new Configurer();
		cm = juf.getService(ConfigurationAdmin.class);
		cfg.setCM(cm);
		cfg.setLogService(juf.getServices(LogService.class).get(0));
		cfg.activate(juf.context);
	}

	@After
	public void after() throws Exception {
		cfg.deactivate();
	}

	@Test
	public void testBasic() throws Exception {
		Bundle bundle = juf.bundle()
				.addResource("configuration/configuration.json",
						new URLResource(getClass().getResource("data/basic.json")))
				.install();

		bundle.start();

		Thread.sleep(DELAY);
		Configuration configuration = cm.getConfiguration("basic");
		assertThat(configuration.getProperties().get("data"), is("data"));
		bundle.uninstall();
	}

	@Test
	public void testOverride() throws Exception {
		Configuration override = cm.getConfiguration("override", "?");
		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put("data", "original");
		override.update(dict);

		Bundle bundle = juf.bundle()
				.addResource("configuration/configuration.json",
						new URLResource(getClass().getResource("data/override.json")))
				.install();

		bundle.start();

		Thread.sleep(DELAY);
		Configuration configuration = cm.getConfiguration("override");
		assertThat(configuration.getProperties().get("data"), is("data"));
		bundle.uninstall();
	}

	@Test
	public void testPrecious() throws Exception {
		Configuration override = cm.getConfiguration("precious", "?");
		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put("a", "original");
		dict.put("b", "original");
		override.update(dict);

		Bundle bundle = juf.bundle()
				.addResource("configuration/configuration.json",
						new URLResource(getClass().getResource("data/precious.json")))
				.install();

		bundle.start();

		Thread.sleep(DELAY);
		Configuration configuration = cm.getConfiguration("precious");
		assertThat(configuration.getProperties().get("a"), is("original"));
		assertThat(configuration.getProperties().get("b"), is("B"));

		bundle.uninstall();
	}

	@Test
	public void testNoOverride() throws Exception {
		Configuration override = cm.getConfiguration("nooverride", "?");
		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put("data", "original");
		override.update(dict);

		Bundle bundle = juf.bundle()
				.addResource("configuration/configuration.json",
						new URLResource(getClass().getResource("data/nooverride.json")))
				.install();

		bundle.start();

		Thread.sleep(DELAY);
		Configuration configuration = cm.getConfiguration("nooverride");
		assertThat(configuration.getProperties().get("data"), is("original"));
		bundle.uninstall();
	}

	@Test
	public void testSystemPropertyExtra() throws Exception {
		Thread.sleep(DELAY);
		Configuration configuration = cm.getConfiguration("system");
		assertThat(configuration.getProperties().get("data"), is("data"));
	}

	@Test
	public void testMacros() throws Exception {
		Bundle bundle = juf.bundle()
				.addResource("configuration/configuration.json",
						new URLResource(getClass().getResource("data/macro.json")))
				.install();
		bundle.start();
		Thread.sleep(DELAY);
		Dictionary<String, Object> configuration = cm.getConfiguration("macro", "?").getProperties();
		assertThat(configuration.get("bundleid"), is(bundle.getBundleId() + ""));
		assertThat(configuration.get("def"), is("--"));
		assertThat((String) configuration.get("location"), startsWith("generated test-"));

		bundle.uninstall();
	}

	@Test
	public void testResource() throws Exception {
		Bundle bundle = juf.bundle()
				.addResource("configuration/configuration.json",
						new URLResource(getClass().getResource("data/resource.json")))
				.addResource("foo.bar",
						new EmbeddedResource("FOO".getBytes(StandardCharsets.UTF_8), 0))
				.install();
		bundle.start();
		Thread.sleep(DELAY);

		Dictionary<String, Object> configuration = cm.getConfiguration("resource", "?").getProperties();
		assertNotNull(  "Configuration must exist", configuration);
		String path = (String) configuration.get("resource");
		File file = new File(path);
		assertTrue( "file must exist", file.isFile());
		String content = IO.collect(file);

		assertThat(content, is("FOO"));

		bundle.uninstall();
	}

	@Test
	public void testProfile() throws Exception {
		Bundle bundle = juf.bundle()
				.addResource("configuration/configuration.json",
						new URLResource(getClass().getResource("data/profile.json")))
				.addResource("foo.bar",
						new EmbeddedResource("FOO".getBytes(StandardCharsets.UTF_8), 0))
				.install();
		bundle.start();
		Thread.sleep(DELAY);

		Configuration configuration = cm.getConfiguration("profile", "?");
		Dictionary<String, Object> dict = configuration.getProperties();
		assertThat(dict.get("foo"), is("FOO"));

		bundle.stop();
		
		Map<String, Object> properties = new HashMap<>();
		properties.put("launcher.arguments", new String[] {"--profile", "bar"});
		cfg.setLauncher(null, properties);

		configuration.update(new Hashtable<>());
		
		Thread.sleep(DELAY);
		bundle.start();
		Thread.sleep(DELAY);
		
		configuration = cm.getConfiguration("profile", "?");
		dict = configuration.getProperties();
		assertThat(dict.get("foo"), is("BAR"));

		bundle.uninstall();
	}
}
