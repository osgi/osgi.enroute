package osgi.enroute.iot.admin.dto;

import java.util.Map;

/**
 * An update send out over Event Admin when values have changed. The properties
 * of this type will be added to the event properties.
 */
public class Delta {
	/**
	 * The system time this delta was created
	 */
	public long time;

	/**
	 * The connector values
	 */
	public Map<Integer, Object> connectorValues;
}
