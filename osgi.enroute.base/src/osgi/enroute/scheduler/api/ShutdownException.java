package osgi.enroute.scheduler.api;

public class ShutdownException extends Throwable {
	private static final long	serialVersionUID	= 1L;

	private ShutdownException() {}
	
	public static ShutdownException SINGLETON = new ShutdownException();

}
