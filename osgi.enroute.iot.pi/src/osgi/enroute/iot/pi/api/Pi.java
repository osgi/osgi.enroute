package osgi.enroute.iot.pi.api;

import org.osgi.annotation.versioning.ProviderType;

import osgi.enroute.iot.pi.provider.PiModelDetector;

/**
 * Service Properties for the {@link PiModelDetector}
 */

@ProviderType
public interface Pi {
	String serial_number();
	String cpu_revision();
	String cpu_model();
	String hardware_revision();
	String board_type();
}
