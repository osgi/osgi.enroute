package osgi.enroute.iot.admin.api;

import java.util.List;

import org.osgi.resource.Wiring;

import osgi.enroute.iot.admin.dto.ICDTO;
import osgi.enroute.iot.admin.dto.WireDTO;

/**
 * A service that can persistently wire ICs together. The Wire Admin can create
 * wires and is responsible for making sure the devices are properly connected.
 */
public interface WireAdmin {

	/**
	 * Generic topic prefix
	 */
	String TOPIC = "/osgi/enroute/iot/";

	/**
	 * A device is added. The appropriate ICDTO is added as properties.
	 */
	String ADD_DEVICE = TOPIC + "add/DEVICE";
	/**
	 * A device is removed. The appropriate ICDTO is added as properties.
	 */
	String REMOVE_DEVICE = TOPIC + "remove/DEVICE";
	/**
	 * A wire is added. The appropriate WireDTO is added as properties.
	 */
	String ADD_WIRE = TOPIC + "add/WIRE";
	/**
	 * A wire is removed. The appropriate WireDTO is added as properties.
	 */
	String REMOVE_WIRE = TOPIC + "remove/WIRE";

	/**
	 * Some of the pin values have changed. Changes are coalesced and batched so
	 * that the updates are minimized. The Delta DTO is added to the properties.
	 */
	String DELTA = TOPIC + "DELTA";

	/**
	 * Get a list of devices
	 */
	List<ICDTO> getDevices();

	/**
	 * Connect two Pins in a wiring
	 * @param wiring the name of the wiring
	 * @param from Connector id
	 * @param to Connector id
	 * @return a new Wire DTO
	 */
	WireDTO connect(String wiring, int from, int to);

	/**
	 * Disconnect a wire in a wiring
	 * @param wiring the name of the wiring
	 * @param wireId the given wireid
	 * @return true if the wire existed
	 */
	boolean disconnect(String wiring, int wireId);

	/**
	 * Get the overall wiring schema for a wiring
	 * @param name the name of the wiring
	 * @return
	 */
	Wiring getWiring(String name);

	/**
	 * Get the names of the existing wirings
	 * @return the list of wiring names
	 */
	List<String> getWirings();

	/**
	 * Remove a wiring
	 * @param wiring the name of the wiring
	 */
	void remove(String wiring);
}
