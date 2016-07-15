package osgi.enroute.web.server.exceptions;

/**
 * Used to indicate that the URI is the name of a folder, but the URI does not
 * end with a "/".
 */
public class FolderException extends WebServerException {
	private static final long	serialVersionUID	= 1L;
	private String				path;

	public FolderException(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}
}
