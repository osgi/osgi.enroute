package osgi.enroute.iot.admin.dto;

import org.osgi.dto.DTO;

/**
 * Represents a connector on a device.
 */
public class PinDTO extends DTO {
	/**
	 * The name of the pin on the device
	 */
	public String name;
	
	/**
	 * The data type of the connector
	 */
	public String type;
	
	/**
	 * The current value
	 */
	
	public boolean value;
}
