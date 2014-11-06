package osgi.enroute.base.required;

import javax.servlet.http.HttpServlet;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;

import osgi.enroute.capabilities.AngularWebResource;
import osgi.enroute.capabilities.BootstrapWebResource;
import osgi.enroute.capabilities.ComponentExtender;
import osgi.enroute.capabilities.ConfigurerExtender;
import osgi.enroute.capabilities.EventAdminSSEEndpoint;
import osgi.enroute.capabilities.ServletWhiteboard;
import osgi.enroute.capabilities.WebServerExtender;
import osgi.enroute.jsonrpc.api.JSONRPC;
import osgi.enroute.rest.api.REST;

/**
 * The purpose of this class is to require all the parts that are part of
 * enRoute base in their right version. This bundle can be deployed in an
 * enRoute distribution to verify that the profile is completely present.
 */
@AngularWebResource.Require
@BootstrapWebResource.Require
@ComponentExtender.Require
@ConfigurerExtender.Require
@EventAdminSSEEndpoint.Require
@ServletWhiteboard.Require
@WebServerExtender.Require
@Component(property = "enroute.profile=base")
public class Base //
		extends HttpServlet // drag in http server
		implements REST, // REST server
		JSONRPC // JSON RPC server
{
	private static final long	serialVersionUID	= 1L;
	
	@Reference
	void setConfigurationAdmin(org.osgi.service.cm.ConfigurationAdmin v) {}

	@Reference
	void setCoordinator(org.osgi.service.coordinator.Coordinator v) {}

	@Reference
	void setEventAdmin(org.osgi.service.event.EventAdmin v) {}

	@Reference
	void setLogService(org.osgi.service.log.LogService v) {}

	@Reference
	void setMetatypeService(org.osgi.service.metatype.MetaTypeService v) {}

	@Reference
	void setUserAdmin(org.osgi.service.useradmin.UserAdmin v) {}

	@Reference
	void setLauncher(osgi.enroute.launch.api.Launcher v) {}

	@Reference
	void setLogger(org.slf4j.Logger v) {}

	@Reference
	void setAuthenticator(osgi.enroute.authentication.api.Authenticator v) {}

	@Reference
	void setAuthority(osgi.enroute.authorization.api.Authority v) {}

	@Reference
	void setConfigurationDone(osgi.enroute.configurer.api.ConfigurationDone v) {}

	@Reference
	void setDTO(osgi.enroute.dto.api.DTOs v) {}

	@Reference
	void setTimer(java.util.Timer v) {}

	@Reference
	void setExecutor(java.util.concurrent.Executor v) {}

	@Reference
	void setHttp(HttpService v) {}

	@Override
	public Object getDescriptor() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
