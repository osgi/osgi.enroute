package osgi.enroute.base.required;


import org.osgi.dto.DTO;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import osgi.enroute.authentication.capabilities.RequireAuthenticationImplementation;
import osgi.enroute.authorization.capabilities.RequireAuthorizationImplementation;
import osgi.enroute.configurer.capabilities.RequireConfigurerExtender;
import osgi.enroute.dto.capabilities.RequireDTOImplementation;
import osgi.enroute.eventadminserversentevents.capabilities.RequireEventAdminServerSentEventsWebResource;
import osgi.enroute.executor.capabilities.RequireExecutorImplementation;
import osgi.enroute.github.angular.capabilities.RequireAngularWebResource;
import osgi.enroute.github.angular_ui.capabilities.RequireAngularUIWebResource;
import osgi.enroute.http.capabilities.RequireHttpImplementation;
import osgi.enroute.iot.admin.capabilities.RequireIotAdminImplementation;
import osgi.enroute.jsonrpc.capabilities.RequireJsonrpcWebResource;
import osgi.enroute.logger.capabilities.RequireLoggerImplementation;
import osgi.enroute.rest.capabilities.RequireRestImplementation;
import osgi.enroute.scheduler.capabilities.RequireSchedulerImplementation;
import osgi.enroute.stackexchange.pagedown.webresource.RequirePagedownWebResource;
import osgi.enroute.twitter.bootstrap.capabilities.RequireBootstrapWebResource;
import osgi.enroute.webserver.capabilities.RequireWebServerExtender;

/**
 * The purpose of this class is to require all the parts that are part of
 * enRoute base in their right version. This bundle can be deployed in an
 * enRoute distribution to verify that the profile is completely present.
 */
@RequireAngularUIWebResource
@RequireAngularWebResource
@RequireAuthenticationImplementation
@RequireAuthorizationImplementation
@RequireBootstrapWebResource
@RequireConfigurerExtender
@RequireDTOImplementation
@RequireEventAdminServerSentEventsWebResource
@RequireExecutorImplementation
@RequireHttpImplementation
@RequireIotAdminImplementation
@RequireJsonrpcWebResource
@RequireLoggerImplementation
@RequirePagedownWebResource
@RequireRestImplementation
@RequireSchedulerImplementation
@RequireWebServerExtender

@Component(property = "enroute.profile=base")
public class Base //
		extends javax.servlet.http.HttpServlet // drag in http server
		implements osgi.enroute.rest.api.REST, // REST server
		osgi.enroute.jsonrpc.api.JSONRPC // JSON RPC server
{
	private static final long	serialVersionUID	= 1L;
	DTO							dto;

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
	void setExecutor(java.util.concurrent.Executor v) {}

	@Reference
	void setHttp(org.osgi.service.http.HttpService v) {}

	@Reference
	void setRSA(org.osgi.service.remoteserviceadmin.RemoteServiceAdmin v) {}

	@Override
	public Object getDescriptor() throws Exception {
		return null;
	}
}
