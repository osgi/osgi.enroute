package osgi.enroute.everything.application;

import org.osgi.service.component.annotations.Component;

import osgi.enroute.capabilities.AngularWebResource;
import osgi.enroute.capabilities.BootstrapWebResource;
import osgi.enroute.capabilities.WebServerExtender;
import osgi.enroute.rest.api.REST;

@AngularWebResource.Require
@BootstrapWebResource.Require
@WebServerExtender.Require
@Component(name = "osgi.enroute.everything")
public class EverythingApplication implements REST {
	public String getUpper(String string) {
		return string.toUpperCase();
	}

}
