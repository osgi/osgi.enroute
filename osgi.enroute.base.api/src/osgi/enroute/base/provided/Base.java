package osgi.enroute.base.provided;


import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import osgi.enroute.capabilities.Unresolvable;

/**
 * This bundle should never resolve. Its intention is to be compile only
 */
@Unresolvable 
@Component(property = "profile=base")
public class Base {

	@Activate
	void activate() {
		System.err.println("THIS IS NOT A RUNNABLE BUNDLE AND SHOULD NOT HAVE BEEN INSTALLED.\n"
				+ "This is an API bundle that is intended to be used for compiling and building. The\n"
				+ "providers of the API should export their API packages.");
	}
}
