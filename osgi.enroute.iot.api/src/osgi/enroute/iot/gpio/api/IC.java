package osgi.enroute.iot.gpio.api;

import osgi.enroute.iot.admin.dto.ICDTO;

public interface IC {
	ICDTO getDTO();
	void fire( String pin, Object value) throws Exception;
}
