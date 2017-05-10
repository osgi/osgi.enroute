package osgi.enroute.base.authorization.test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

import aQute.bnd.annotation.component.Reference;
import aQute.bnd.testing.DSTestWiring;
import junit.framework.TestCase;
import osgi.enroute.authorization.api.Authority;
import osgi.enroute.authorization.api.AuthorityAdmin;
import osgi.enroute.authorization.api.SecurityVerifier;
import osgi.enroute.base.configurer.test.ConfigurerTest;

public class AuthorizationTest extends TestCase {

	BundleContext context = FrameworkUtil.getBundle(ConfigurerTest.class)
			.getBundleContext();
	DSTestWiring ds = new DSTestWiring();
	private Authority authority;
	private AuthorityAdmin admin;
	private UserAdmin userAdmin;

	public void setUp() throws Exception {
		ds.setContext(context);
		ds.add(this);
		ds.wire();

		userAdmin.removeRole("peter@aqute.biz");
		userAdmin.removeRole("admin");
		userAdmin.removeRole("admin;x*");
	}

	/**
	 * Very little we can actually test since this depends on external state and
	 * secrets ...
	 */
	interface SomeDomain {
		boolean admin();

		boolean admin(String arg);

		boolean nothing(String arg);
	};

	public void testAuthorization() throws Exception {
		assertNotNull(admin);
		assertNotNull(authority);
		assertNotNull(userAdmin);

		User peter = (User) userAdmin.createRole("peter", Role.USER);
		Group admin = (Group) userAdmin.createRole("admin", Role.GROUP);
		Group adminDomain = (Group) userAdmin
				.createRole("admin;x*", Role.GROUP);

		admin.addMember(peter);
		adminDomain.addMember(peter);

		Authorization auth = userAdmin.getAuthorization(peter);
		List<String> roles = Arrays.asList(auth.getRoles());
		assertNotNull(roles);
		assertEquals(3, roles.size());

		assertTrue(roles.contains("admin"));
		assertTrue(roles.contains("admin;x*"));

		this.admin.call("peter", new Callable<Integer>() {

			@Override
			public Integer call() throws Exception {

				assertEquals("peter", authority.getUserId());

				// By hand verifier

				authority.checkPermission("admin");
				authority.checkPermission("admin", "xyz");

				assertFalse(authority.hasPermission("admin", "abc"));
				assertFalse(authority.hasPermission("nothing", "abc"));

				// Security verifier

				SomeDomain security = SecurityVerifier.createVerifier(
						SomeDomain.class, authority);

				assertTrue( 	security.admin() 		);
				assertTrue( 	security.admin("xyz") 	);
				assertFalse( 	security.admin("abc") 	);
				assertFalse( 	security.nothing("abc")	);

				return null;
			}
		});
	}

	@Reference
	void setAuthority(Authority authority) {
		this.authority = authority;
	}

	@Reference
	void setAuthorityAdmin(AuthorityAdmin admin) {
		this.admin = admin;
	}

	@Reference
	void setUserAdmin(UserAdmin userAdmin) {
		this.userAdmin = userAdmin;
	}
}
