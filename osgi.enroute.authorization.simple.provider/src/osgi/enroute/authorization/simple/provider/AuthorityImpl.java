package osgi.enroute.authorization.simple.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.osgi.dto.DTO;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;

import osgi.enroute.authorization.api.Authority;
import osgi.enroute.authorization.api.AuthorityAdmin;
import osgi.enroute.authorization.api.AuthorizationConstants;
import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.libg.glob.Glob;

@ProvideCapability(ns=ImplementationNamespace.IMPLEMENTATION_NAMESPACE, name=AuthorizationConstants.AUTHORIZATION_SPECIFICATION_NAME, version=AuthorizationConstants.AUTHORIZATION_SPECIFICATION_VERSION)
@Component
public class AuthorityImpl implements Authority, AuthorityAdmin {
	private static final Glob[]				EMPTY_GLOBS	= new Glob[0];

	@Reference
	private Logger							log;
	@Reference
	private UserAdmin						userAdmin;

	@Reference
	volatile List<UserAdmin>						userAdmins = new ArrayList<UserAdmin>();
	
	private ThreadLocal<SecurityContext>	context		= new ThreadLocal<>();

	static class SecurityContext extends DTO {
		public String						userId;
		public Thread						onThread;
		public long							started;
		public NavigableMap<String,Assert>	asserts	= new TreeMap<>();
	}

	static class Assert {
		final Glob[]	matchers;

		public Assert(String key) {
			String[] parts = key.split(";");
			matchers = parts.length == 0 ? EMPTY_GLOBS : new Glob[parts.length - 1];
			for (int i = 1; i < parts.length; i++) {
				matchers[i - 1] = new Glob(parts[i]);
			}
		}

		public boolean matches(String[] arguments) {

			if (matchers.length != arguments.length)
				return false;

			for (int i = 0; i < matchers.length; i++)
				if (!matchers[i].matcher(arguments[i]).matches())
					return false;

			return true;
		}
	}

	@Activate
	void activate() {

	}

	@Override
	public String getUserId() throws Exception {
		SecurityContext context = this.context.get();
		if (context == null)
			return null;

		return context.userId;
	}

	@Override
	public List<String> getPermissions() throws Exception {
		SecurityContext context = this.context.get();
		if (context == null)
			return null;

		return new ArrayList<>(context.asserts.keySet());
	}

	@Override
	public boolean hasPermission(String permission, String... arguments) throws Exception {
		SecurityContext context = this.context.get();
		if (context == null)
			return false;

		NavigableMap<String,Assert> tailMap = context.asserts.tailMap(permission, true);
		for (Map.Entry<String,Assert> entry : tailMap.entrySet()) {

			String key = entry.getKey();

			if (!key.startsWith(permission))
				break;

			if (permission.length() < key.length() && key.charAt(permission.length()) != ';')
				break;

			Assert a = entry.getValue();
			if (a == null) {
				a = new Assert(key);
				entry.setValue(a);
			}

			if (a.matches(arguments))
				return true;
		}
		return false;
	}

	@Override
	public void checkPermission(String permission, String... arguments) throws Exception {
		if (hasPermission(permission, arguments))
			return;

		log.warn("User %s fails to get permission %s with args %s", permission, arguments);

		if (arguments.length == 0)
			throw new SecurityException("User " + getUserId() + " does not have permission " + permission);

		throw new SecurityException("User " + getUserId() + " does not have permission " + permission
				+ " for arguments " + Arrays.toString(arguments));
	}

	@Override
	public <T> T call(String userId, Callable<T> task) throws Exception {
		SecurityContext context = new SecurityContext();

		try {
			if (userId == null)
				userId = Role.USER_ANYONE;

			Role role = userAdmin.getRole(userId);
			if (role == null)
				throw new IllegalArgumentException("No such user " + userId);

			if (!(role instanceof User))
				throw new IllegalArgumentException("This id is not for a User " + userId);

			User user = (User) role;

			context.userId = user.getName();
			context.onThread = Thread.currentThread();
			context.started = System.currentTimeMillis();

			Authorization authorization = userAdmin.getAuthorization(user);

			String[] roleNames = authorization.getRoles();

			if (roleNames != null) {
				for (String roleName : roleNames) {
					context.asserts.put(roleName, null);
				}
			}

			this.context.set(context);

			return task.call();

		}
		catch (Exception e) {
			log.error("Invocation for user " + userId + " failed", e);
			throw e;
		}
		finally {
			this.context.set(null);
		}
	}
}
