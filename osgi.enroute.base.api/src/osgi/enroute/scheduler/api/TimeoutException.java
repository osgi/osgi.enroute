package osgi.enroute.scheduler.api;

/**
 * The singleton exception thrown when a task times out
 */
public class TimeoutException extends Throwable {
	private static final long	serialVersionUID	= 1L;

	private TimeoutException() {}

	/**
	 * The singleton timeout exception
	 */
	public static TimeoutException SINGLETON = new TimeoutException();
	
}
