package osgi.enroute.scheduler.api;

public class TimeoutException extends Throwable {
	private static final long	serialVersionUID	= 1L;

	private TimeoutException() {}
	
	public static TimeoutException SINGLETON = new TimeoutException();
	
}
