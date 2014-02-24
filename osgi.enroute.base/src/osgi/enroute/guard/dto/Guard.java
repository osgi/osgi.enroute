package osgi.enroute.guard.dto;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import osgi.enroute.dto.api.DTO;
import aQute.bnd.version.Version;

/**
 * 
 *
 */
public class Guard extends DTO {
	public static class Requirement extends DTO {
		public String				ns;
		public String				filter;
		public Map<String,Object>	attributes;
		public Map<String,Object>	directives;
		public String				description;
		public String				effective;
	}

	public String			name;
	public String			description;
	public Set<Requirement>	requirements	= new LinkedHashSet<>();
	public Version	version;
}
