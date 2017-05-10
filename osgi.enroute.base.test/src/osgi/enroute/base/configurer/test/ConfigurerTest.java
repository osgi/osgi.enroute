package osgi.enroute.base.configurer.test;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

import aQute.bnd.annotation.component.Reference;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.URLResource;
import aQute.bnd.testing.DSTestWiring;
import junit.framework.TestCase;
import osgi.enroute.configurer.api.ConfigurationDone;
import osgi.enroute.configurer.api.RequireConfigurerExtender;

@RequireConfigurerExtender
public class ConfigurerTest extends TestCase {
	BundleContext				context	= FrameworkUtil.getBundle(ConfigurerTest.class).getBundleContext();
	DSTestWiring				ds		= new DSTestWiring();
	private ConfigurationAdmin	cm;

	public void setUp() throws Exception {
		ds.setContext(context);
		ds.add(this);
		ds.wire();
	}

	public void testSimple() throws Exception {
		final Semaphore s = new Semaphore(0);

		context.registerService(ConfigurationListener.class, new ConfigurationListener() {

			@Override
			public void configurationEvent(ConfigurationEvent event) {
				switch (event.getPid()) {
					case "singleton" :
						s.release();
						break;

					default :
						if (event.getFactoryPid().equals("factory")) {
							s.release();
						}
						break;
				}
			}
		}, null);

		Bundle a = doBundleFor("a.json");

		assertEquals("check if we're running", Bundle.ACTIVE, a.getState());

		//
		// See if we got our 3 updates
		//
		
		if (!s.tryAcquire(3, 100, TimeUnit.SECONDS)) {
			fail("Cannot find updates");
		}

		checkConfigs(1,2);
		
		//
		// Check that the configs are persistent
		//
		
		a.uninstall();
		
		checkConfigs(1,2);
		
		
		//
		// Now we make a new bundle that will set a new port for the singleton (10)
		// and will change the first factory, ignore the second, and add a third
		//
		
		Bundle b = doBundleFor("b.json");
		
		if (!s.tryAcquire(4, 100, TimeUnit.SECONDS)) {
			fail("Cannot find updates");
		}
		
		checkConfigs(10,3);
		b.uninstall();
		
	}

	private void checkConfigs(int port, int factories) throws IOException, InvalidSyntaxException {
		Configuration c = cm.getConfiguration("singleton");
		assertNotNull(c);
		assertEquals( port, c.getProperties().get("port"));
		
		Configuration[] list = cm.listConfigurations("(service.factoryPid=factory)");
		assertNotNull( list	 );
		assertEquals( factories, list.length);
	}

	private Bundle doBundleFor(String resource) throws Exception, BundleException {
		try (Builder b = new Builder()) {

			b.setBundleSymbolicName("test." + resource);
			b.setProperty(ConfigurationDone.BUNDLE_CONFIGURATION, "configuration/configuration.json");
			b.build();
			b.getJar().putResource("configuration/configuration.json",
					new URLResource(getClass().getResource(resource)));

			Bundle a = context.installBundle("test", new JarResource(b.getJar()).openInputStream());
			a.start();
			return a;
		}
	}

	@Reference
	void setCM(ConfigurationAdmin cm) {
		this.cm = cm;
	}
}
