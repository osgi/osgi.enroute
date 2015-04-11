package osgi.enroute.iot.admin.dto;

import java.net.URI;
import java.util.List;

import org.osgi.dto.DTO;

/**
 * Represents a DTO for describing an IC
 */
public class ICDTO extends DTO {
	/**
	 * The unique IC id
	 */
	public int deviceId;

	/**
	 * The IC type, the java implementation class name.
	 */
	public String type;

	/**
	 * The human readable name of the device
	 */
	public String name;

	/**
	 * The optional icon URI, will return null if no icon
	 */
	public URI icon;

	/**
	 * The input Pins on this device
	 */
	public List<PinDTO> inputs;
	/**
	 * The output Pins on this device
	 */
	public List<PinDTO> outputs;
}
