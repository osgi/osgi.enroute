package osgi.enroute.iot.admin.dto;

import org.osgi.dto.DTO;

/**
 * Represents a connector on a device.
 */
public class PinDTO extends DTO {
	/**
	 * Unique within a WireAdmin ID for the connector.
	 */
	public int connectorId;
	
	/**
	 * The unique device id. This is redundant but a nice convenience.
	 */
	public int deviceId;
	
	/**
	 * The name of the connector on the device
	 */
	public String name;
	
	/**
	 * If this is input or output
	 */
	public boolean input;
	
	/**
	 * The data type of the connector
	 */
	public String type;
}
