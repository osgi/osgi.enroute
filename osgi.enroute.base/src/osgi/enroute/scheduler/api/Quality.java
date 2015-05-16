package osgi.enroute.scheduler.api;

import org.osgi.dto.DTO;

public class Quality extends DTO {
	public int maxFires;
	public int priority;
	public int	repeatsAfterException	= 0;
	public boolean executeEachFiring;
	
}
