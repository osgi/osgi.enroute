package osgi.enroute.iot.admin.api;

import java.util.List;

import osgi.enroute.iot.admin.dto.Delta;
import osgi.enroute.iot.admin.dto.ICDTO;
import osgi.enroute.iot.admin.dto.WireDTO;

/**
 * A service that can persistently wire ICs together. The Circuit Admin can create
 * wires and is responsible for making sure the devices are properly connected.
 */
public interface CircuitAdmin {

	/**
	 * Generic topic prefix for Event Admin. The properties of the event are specified in {@link Delta}.
	 */
	String TOPIC = "osgi/enroute/iot/circuit/DELTA";


	/**
	 * Get a list of devices
	 */
	ICDTO[] getICs();

	/**
	 * Connect two Pins in a wiring
	 * 
	 * @param wiring
	 *            the name of the wiring
	 * @param from
	 *            Connector id
	 * @param to
	 *            Connector id
	 * @return a new Wire DTO
	 */
	WireDTO connect(String fromDevice, String fromPin, String toDevice, String toPin) throws Exception;

	/**
	 * Disconnect a wire in a wiring
	 * 
	 * @param wireId
	 *            the given wireid
	 * @return true if the wire existed
	 */
	boolean disconnect(int wireId) throws Exception;

	/**
	 * Return a list of wires
	 * @return a list of wires
	 */
	List<WireDTO> getWires();
}
