package osgi.enroute.base.required;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import osgi.enroute.component.api.RequireComponentExtender;
import osgi.enroute.configurer.api.RequireConfigurerExtender;

/**
 * The purpose of this class is to require all the parts that are part of
 * enRoute base in their right version. This bundle can be deployed in an
 * enRoute distribution to verify that the profile is completely present.
 */
@RequireComponentExtender
@RequireConfigurerExtender
@Component(properties = "enroute.profile=base")
public class Base {
	
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
}
