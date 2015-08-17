package osgi.enroute.iot.admin.dto;

import java.util.Map;

/**
 * An update send out over Event Admin when values have changed. The properties
 * of this type will be added to the event properties.
 */
public class Delta {
	public long time;
	public Map<Integer, Object> connectorValues;
}
