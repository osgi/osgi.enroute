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
	 * From device id
	 */
	public String fromDevice;

	/**
	 * From pin
	 * 
	 */
	public String fromPin;

	/**
	 * To device id
	 */
	public String toDevice;

	/**
	 * To device pin
	 */
	public String toPin;
	
	/**
	 * The wire is actually wiring two pins.
	 */
	public boolean wired = false;
}
