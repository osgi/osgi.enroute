package osgi.enroute.logger.simple.provider;

import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Log messages are queued through the LoggerDispatcher. This is the content of
 * that message.
 */
class Entry extends DTO {
	public Bundle					source;
	public String					message;
	public int						level;
	public ServiceReference< ? >	reference;
	public Throwable				exception;
}
