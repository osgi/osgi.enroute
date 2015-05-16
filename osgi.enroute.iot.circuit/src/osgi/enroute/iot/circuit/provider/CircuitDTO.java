package osgi.enroute.iot.circuit.provider;

import java.util.Collection;

import org.osgi.dto.DTO;

/**
 * Type for storing the circuit on disk
 */
public class CircuitDTO extends DTO {
	Collection<WireImpl>		wires;
}
