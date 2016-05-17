package osgi.enroute.twitter.provider.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

public class SearchResult extends DTO {
	public List<Tweet>		statuses;
	public SearchMetadata	search_metadata;

	Map<String, Object>		__extra	= new HashMap<String, Object>();
}
