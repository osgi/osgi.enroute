package osgi.enroute.twitter.provider.dto;

import java.util.HashMap;
import java.util.Map;

import org.osgi.dto.DTO;

public class Tweet extends DTO {
	public String				created_at;
	public String				text;
	public long					id;
	public String				id_str;
	public boolean				truncated;
	public Map<String, Object>	hashtags;

	Map<String, Object>			__extra	= new HashMap<String, Object>();
}
