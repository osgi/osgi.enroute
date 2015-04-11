package osgi.enroute.iot.admin.dto;

import java.util.List;

import org.osgi.dto.DTO;

/**
 * A DTO for all the devices and wires
 */
public class WiringDTO extends DTO {
	public String name;
	public List<ICDTO> devices;
	public List<WireDTO> wires;
}
