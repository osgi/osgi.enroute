package osgi.enroute.scheduler.api;

/**
 * Singleton exception thrown when a scheduler is shutdown and kills the tasks
 */
public class ShutdownException extends Throwable {
	private static final long	serialVersionUID	= 1L;

	private ShutdownException() {}
	
	/**
	 * The singleton exception
	 */
	public static ShutdownException SINGLETON = new ShutdownException();

}
