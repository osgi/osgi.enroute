package osgi.enroute.iot.circuit.application;

import java.util.Collection;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import osgi.enroute.configurer.api.RequireConfigurerExtender;
import osgi.enroute.eventadminserversentevents.capabilities.RequireEventAdminServerSentEventsWebResource;
import osgi.enroute.google.angular.capabilities.RequireAngularWebResource;
import osgi.enroute.iot.admin.api.CircuitAdmin;
import osgi.enroute.iot.admin.dto.ICDTO;
import osgi.enroute.iot.admin.dto.WireDTO;
import osgi.enroute.jsonrpc.api.JSONRPC;
import osgi.enroute.twitter.bootstrap.capabilities.RequireBootstrapWebResource;
import osgi.enroute.webserver.capabilities.RequireWebServerExtender;

/**
 * Main application class for the Circuit editor/viewer.
 */
@RequireAngularWebResource(resource={"angular.js","angular-resource.js", "angular-route.js"}, priority=1000)
@RequireBootstrapWebResource(resource="css/bootstrap.css")
@RequireWebServerExtender
@RequireConfigurerExtender
@RequireEventAdminServerSentEventsWebResource
@Component(name="osgi.enroute.iot.circuit", property = JSONRPC.ENDPOINT + "=osgi.enroute.iot.circuit")
public class CircuitApplication implements JSONRPC {

	private CircuitAdmin ca;

	@Override
	public Object getDescriptor() throws Exception {
		return "";
	}
	
	public Collection<? extends WireDTO> getWires() {
		return ca.getWires();
	}

	public ICDTO[] getDevices() {
		return ca.getICs();
	}

	public boolean disconnect(int wireId) throws Exception {
		return ca.disconnect(wireId);
	}

	public WireDTO connect(String fromDevice, String fromPin, String toDevice, String toPin) throws Exception {
		return ca.connect(fromDevice, fromPin, toDevice, toPin);
	}

	@Reference
	void setCircuitAdmin(CircuitAdmin ca) {
		this.ca = ca;
	}

}
