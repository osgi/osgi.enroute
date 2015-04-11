package osgi.enroute.iot.emulator.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import osgi.enroute.dto.api.DTOs;

/**
 * 
 */
@Component(name = "osgi.enroute.iot.emulator")
public class EmulatorImpl {
	final Map<Integer,GPIO> gpio= new ConcurrentHashMap<Integer, GPIO>();
	interface Configuration {
		String[] gpio();
	}
	Configuration config;
	private DTOs dtos;
	
	@Activate
	void activate(Map<String,Object> map) throws Exception {
		modify(map);
	}
	
	@Modified
	void modify(Map<String,Object> map) throws Exception {
		config = dtos.convert(map).to(Configuration.class);
	}


	@Reference
	void setDTOs(DTOs dtos) {
		this.dtos=dtos;
	}
}
