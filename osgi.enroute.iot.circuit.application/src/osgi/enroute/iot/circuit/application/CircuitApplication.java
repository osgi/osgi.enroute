package osgi.enroute.iot.circuit.application;

import java.util.Collection;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import osgi.enroute.capabilities.AngularWebResource;
import osgi.enroute.capabilities.BootstrapWebResource;
import osgi.enroute.capabilities.ConfigurerExtender;
import osgi.enroute.capabilities.EventAdminSSEEndpoint;
import osgi.enroute.capabilities.WebServerExtender;
import osgi.enroute.iot.admin.api.CircuitAdmin;
import osgi.enroute.iot.admin.dto.ICDTO;
import osgi.enroute.iot.admin.dto.WireDTO;
import osgi.enroute.jsonrpc.api.JSONRPC;

/**
 * Main application class for the Circuit editor/viewer.
 */
@AngularWebResource(resource={"angular.js","angular-resource.js", "angular-route.js"}, priority=1000)
@BootstrapWebResource(resource="css/bootstrap.css")
@WebServerExtender
@ConfigurerExtender
@EventAdminSSEEndpoint
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
