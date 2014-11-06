package osgi.enroute.base.provided;


import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import osgi.enroute.capabilities.AngularWebResource;
import osgi.enroute.capabilities.BootstrapWebResource;
import osgi.enroute.capabilities.ComponentExtender;
import osgi.enroute.capabilities.ConfigurerExtender;
import osgi.enroute.capabilities.EventAdminSSEEndpoint;
import osgi.enroute.capabilities.ServletWhiteboard;
import osgi.enroute.capabilities.Unresolvable;
import osgi.enroute.capabilities.WebServerExtender;

/*
 * This bundle should never resolve. Its intention is to be compile only 
 */
@Unresolvable 
@AngularWebResource.Provide
@BootstrapWebResource.Provide
@ComponentExtender.Provide
@ConfigurerExtender.Provide
@EventAdminSSEEndpoint.Provide
@ServletWhiteboard.Provide
@WebServerExtender.Provide
@Component(property = "profile=base")
public class Base {

	@Activate
	public void activate() {
		System.err.println("THIS IS NOT A RUNNABLE BUNDLE AND SHOULD NOT HAVE BEEN INSTALLED.\n"
				+ "This is an API bundle that is intended to be used for compiling and building. The\n"
				+ "providers of the API should export their API packages.");
	}
}
