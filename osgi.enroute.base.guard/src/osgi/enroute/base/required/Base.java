package osgi.enroute.base.required;


import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import osgi.enroute.capabilities.AngularUIWebResource;
import osgi.enroute.capabilities.AngularWebResource;
import osgi.enroute.capabilities.BootstrapWebResource;
import osgi.enroute.capabilities.ComponentExtender;
import osgi.enroute.capabilities.ConfigurerExtender;
import osgi.enroute.capabilities.EventAdminSSEEndpoint;
import osgi.enroute.capabilities.PagedownWebResource;
import osgi.enroute.capabilities.ServletWhiteboard;
import osgi.enroute.capabilities.WebServerExtender;

/**
 * The purpose of this class is to require all the parts that are part of
 * enRoute base in their right version. This bundle can be deployed in an
 * enRoute distribution to verify that the profile is completely present.
 */
@AngularUIWebResource
@AngularWebResource
@BootstrapWebResource
@ComponentExtender
@ConfigurerExtender
@EventAdminSSEEndpoint
@PagedownWebResource
@ServletWhiteboard
@WebServerExtender
@Component(property = "enroute.profile=base")
public class Base //
		extends javax.servlet.http.HttpServlet // drag in http server
		implements osgi.enroute.rest.api.REST, // REST server
		osgi.enroute.jsonrpc.api.JSONRPC // JSON RPC server
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
	void setLoggerAdmin(osgi.enroute.logger.api.LoggerAdmin v) {}

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
	void setScheduler(osgi.enroute.scheduler.api.Scheduler s) {}

	@Reference
	void setTimer(java.util.Timer v) {}

	@Reference
	void setExecutor(java.util.concurrent.Executor v) {}

	@Reference
	void setHttp(org.osgi.service.http.HttpService v) {}

	@Override
	public Object getDescriptor() throws Exception {
		return null;
	}
}
