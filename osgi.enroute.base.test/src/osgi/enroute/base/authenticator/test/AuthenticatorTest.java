package osgi.enroute.base.authenticator.test;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import aQute.bnd.annotation.component.Reference;
import aQute.bnd.testing.DSTestWiring;
import junit.framework.TestCase;
import osgi.enroute.authentication.api.Authenticator;
import osgi.enroute.base.configurer.test.ConfigurerTest;

public class AuthenticatorTest extends TestCase {

	BundleContext			context	= FrameworkUtil.getBundle(ConfigurerTest.class).getBundleContext();
	DSTestWiring			ds		= new DSTestWiring();
	private Authenticator	authenticator;

	public void setUp() throws Exception {
		ds.setContext(context);
		ds.add(this);
		ds.wire();
	}

	/**
	 * Very little we can actually test since this depends on external state and
	 * secrets ...
	 */
	public void testAuthenticator() throws Exception {
		assertNotNull(authenticator);
	}

	@Reference
	void setAuthenticator(Authenticator authenticator) {
		this.authenticator = authenticator;
	}

}
