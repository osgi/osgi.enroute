package osgi.enroute.web.server.provider;

public class RedirectException extends RuntimeException {
	private static final long	serialVersionUID	= 1L;
	private String				path;

	public RedirectException(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}
}
