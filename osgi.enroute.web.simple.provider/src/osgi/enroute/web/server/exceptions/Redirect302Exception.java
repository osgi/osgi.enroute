package osgi.enroute.web.server.exceptions;

public class Redirect302Exception extends WebServerException {
	private static final long	serialVersionUID	= 1L;
	private String				path;

	public Redirect302Exception(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}
}
