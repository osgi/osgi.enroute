package osgi.enroute.iot.circuit.provider;

import java.util.Collection;

import org.osgi.dto.DTO;

/**
 * Type for storing the circuit on disk. Know it is just a list but we might
 * want to add other stuff in the future.
 */
public class CircuitDTO extends DTO {
	public Collection<WireImpl>	wires;
}
