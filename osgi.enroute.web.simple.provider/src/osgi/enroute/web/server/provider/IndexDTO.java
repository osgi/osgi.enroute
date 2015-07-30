package osgi.enroute.web.server.provider;

import java.util.*;

import org.osgi.dto.*;

public class IndexDTO extends DTO {

	public List<ApplicationDTO>	applications	= new ArrayList<>();
	public Map<String,Object> configuration;
	
	public static class ApplicationDTO extends DTO {
		public long			bundle;
		public String		name;
		public String		bsn;
		public String		version;
		public String		link;
		public String		description;
	}
}
