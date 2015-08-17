package osgi.enroute.iot.admin.dto;

import java.net.URI;

import org.osgi.dto.DTO;

/**
 * Represents a DTO for describing an IC
 */
public class ICDTO extends DTO {
	/**
	 * The unique IC id, generally the PID
	 */
	public String deviceId;

	/**
	 * The IC type, the java implementation class name.
	 */
	public String type;

	/**
	 * The human readable name of the device
	 */
	public String name;

	/**
	 * The optional icon URI, will be null if no icon
	 */
	public URI icon;

	/**
	 * The input Pins on this device
	 */
	public PinDTO inputs[];
	/**
	 * The output Pins on this device
	 */
	public PinDTO outputs[];
}
