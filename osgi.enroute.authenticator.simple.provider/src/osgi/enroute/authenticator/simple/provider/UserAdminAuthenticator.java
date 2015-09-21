package osgi.enroute.authenticator.simple.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;

import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.lib.base64.Base64;
import aQute.lib.hex.Hex;
import osgi.enroute.authentication.api.AuthenticationConstants;
import osgi.enroute.authentication.api.Authenticator;
import osgi.enroute.authenticator.simple.provider.Config.Algorithm;
import osgi.enroute.debug.api.Debug;

@ProvideCapability(ns=ImplementationNamespace.IMPLEMENTATION_NAMESPACE, name=AuthenticationConstants.AUTHENTICATION_SPECIFICATION_NAME, version=AuthenticationConstants.AUTHENTICATION_SPECIFICATION_VERSION)
@Component(property = {
        Debug.COMMAND_SCOPE + "=user", Debug.COMMAND_FUNCTION + "=hash", Debug.COMMAND_FUNCTION + "=passwd", Debug.COMMAND_FUNCTION + "=adduser",
		Debug.COMMAND_FUNCTION + "=rmrole", Debug.COMMAND_FUNCTION + "=role", Debug.COMMAND_FUNCTION + "=user"
})
public class UserAdminAuthenticator implements Authenticator {
	private static final Pattern	AUTHORIZATION_P	= Pattern.compile("Basic\\s+(?<base64>[A-Za-z0-9+/]{3,}={0,2})");
	private static final Pattern	IDPW_P			= Pattern.compile("(?<id>[^:]+):(?<pw>.*)");
	private UserAdmin				userAdmin;
	private Logger					log;

	private byte[]					salt;
	private Algorithm				algorithm;
	private int						iterations;
	private String					root;

	@Activate
	void activate(Config config) throws Exception {
		salt = config.salt();
		if (salt == null || salt.length == 0) {
			salt = new byte[] {
					0x2f, 0x68, (byte) 0xcb, 0x75, 0x6c, (byte) 0xf1, 0x74, (byte) 0x84, 0x2a, (byte) 0xef
			};
		}

		algorithm = config.algorithm();
		if (algorithm == null) {
			algorithm = Algorithm.PBKDF2WithHmacSHA1;
		}

		iterations = config.iterations();
		if (iterations < 100)
			iterations = 997;

		root = config._root();
		if (root != null && root.trim().isEmpty())
			root = null;
	}

	@Override
	public String authenticate(Map<String,Object> arguments, String... sources) throws Exception {

		for (String source : sources) {

			if (Authenticator.BASIC_SOURCE_PASSWORD.equals(source)) {
				String id = (String) arguments.get(Authenticator.BASIC_SOURCE_USERID);
				String pw = (String) arguments.get(Authenticator.BASIC_SOURCE_PASSWORD);
				if (id != null && pw != null)
					return verify(id, pw);

				log.info("BASIC_SOURCE_PASSWORD specified but no userid/password found in arguments");
			}

			if (Authenticator.SERVLET_SOURCE.equals(source)) {
				String uri = (String) arguments.get(Authenticator.SERVLET_SOURCE_METHOD);
				if (uri.startsWith("https:")) {
					TreeMap<String,Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
					map.putAll(arguments);

					String auth = (String) map.get("Authorization");
					if (auth != null) {
						Matcher m = AUTHORIZATION_P.matcher(auth);

						if (m.find()) {
							String base64 = m.group("base64");
							byte[] bytes = Base64.decodeBase64(base64);
							String pwId = new String(bytes, "UTF-8");
							m = IDPW_P.matcher(pwId);
							if (m.matches()) {
								String id = m.group("id");
								String pw = m.group("pw");
								return verify(id, pw);
							}
						}

						// assume it is not Basic but something else

					} else {
						log.warn("Servlet authentication requires an Authorization header");
					}
				} else {
					log.warn("Servlet authentication requires https {}", uri);
				}
			}
		}
		return null;
	}

	@Override
	public boolean forget(String userid) throws Exception {
		return false;
	}

	private String verify(String id, String pw) throws Exception {
		Role role = userAdmin.getRole(id);
		if (role == null) {
			log.info("Failed login attempt for %s: no such user", id);
			return null;
		}

		if (!(role instanceof User)) {
			log.info("Failed login attempt for %s: id is not a user name but %s", id, role);
			return null;
		}

		User user = (User) role;

		String hash = hash(pw);
		if (user.hasCredential(algorithm.toString(), hash))
			return id;

		if (root != null && root.equals(hash)) {
			log.info("Root login by %s", id);
			return id;
		}

		log.info("Failed login attempt for %s: invalid password", id);
		return null;
	}

	public String hash(String password) throws Exception {
		switch (algorithm) {
			default :
			case PBKDF2WithHmacSHA1 :
				byte[] hash = pbkdf2(password.toCharArray(), salt, iterations, 24);
				return Hex.toHexString(hash);
		}
	}

	byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bytes) throws Exception {
		PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
		SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm.toString());
		return skf.generateSecret(spec).getEncoded();
	}

	public Role adduser(String id) {
		// TODO validate id
		return userAdmin.createRole(id, Role.USER);
	}

	public List<Role> role(String... filter) throws InvalidSyntaxException {
		List<Role> roles = new ArrayList<>();

		if (filter.length == 0) {
			getRoles(roles, null);
		} else {

			for (String f : filter) {
				if (f.startsWith("(") && f.endsWith(")")) {
					getRoles(roles, f);
				} else {
					Role r = userAdmin.getRole(f);
					if (f != null)
						roles.add(r);
				}
			}
		}
		return roles;
	}

	private void getRoles(List<Role> roles, String filter) throws InvalidSyntaxException {
		Role[] rs = userAdmin.getRoles(filter);
		if (rs != null) {
			for (Role role : rs) {
				roles.add(role);
			}
		}
	}

	public int rmrole(String... id) {
		int n = 0;
		for (String i : id) {
			userAdmin.removeRole(i);
			n++;
		}
		return n;
	}

	
	public String user() {
		return "User Admin commands\n"
				+ "  hash <password>                        Create the hash of a password\n"
				+ "  passwd <id> <password>                 Set password\n"
				+ "  adduser <id>                           Create a user\n"
				+ "  rmrole                                 Remove a role\n"
				+ "  role                                   List the roles\n"
				+ "\n";
	}
	@SuppressWarnings("unchecked")
	public void passwd(String id, String pw) throws Exception {
		Role role = userAdmin.getRole(id);

		if (role == null) {
			role = userAdmin.createRole(id, Role.USER);
		} else if (!(role instanceof User)) {
			System.err.println("Not a user role, but " + role);
			return;
		}

		User user = (User) role;

		user.getCredentials().put(algorithm.toString(), hash(pw));
	}

	@Reference
	void setUA(UserAdmin userAdmin) {
		this.userAdmin = userAdmin;
	}

	@Reference
	void setLog(Logger logger) {
		this.log = logger;
	}

}
