package osgi.enroute.web.server.exceptions;

public class NotFound404Exception extends WebServerException {
	private static final long	serialVersionUID	= 1L;

	private final String bsn;

	public NotFound404Exception(String bsn) {
		super();
		this.bsn = bsn;
	}

	public NotFound404Exception(String bsn, Throwable cause) {
		super(cause);
		this.bsn = bsn;
	}

	public String getBundleSymbolicName()
	{
		return bsn;
	}
}
