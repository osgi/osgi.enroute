package osgi.enroute.everything.application;

import org.osgi.service.component.annotations.Component;

import osgi.enroute.capabilities.WebServerExtender;
import osgi.enroute.debug.api.Debug;

@WebServerExtender.Require
@Component(service=EverythingApplication.class, property = { Debug.COMMAND_SCOPE + "=evrthng",
	Debug.COMMAND_FUNCTION + "=evrthng" },name="osgi.enroute.everything")
public class EverythingApplication {

	
	/*
	 * Gogo command
	 */
	public String evrthng(String m) throws Exception {
		return m;
	}
}
