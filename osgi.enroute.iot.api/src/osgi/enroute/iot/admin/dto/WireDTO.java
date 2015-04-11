package osgi.enroute.iot.admin.dto;

import org.osgi.dto.DTO;

/**
 * Represents a connection between two pins.
 */
public class WireDTO extends DTO {
	/**
	 * Unique id for this wire
	 */
	public int wireId;

	/**
	 * The from Connector id
	 */
	public int from;
	/**
	 * The to Connector id
	 */
	public int to;
}
