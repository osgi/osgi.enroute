package osgi.enroute.rest.api;

import java.net.URI;

/**
 * A class to represent a response for a REST request. Instances can be thrown
 * since it extends exceptions.
 * 
 * The intention of this class is to create a ReponseObject in OpenAPI
 * (swagger). All information is available through reflection. For this reason,
 * a subclass should not have any sideeffects when instantiated (and it must
 * have an empty constructor).
 *
 * A subclass can add public fields that are treated as response headers. In the
 * field names the '_' is treated as a dash ('-'). Any other ASCII character
 * (including the underscore) can be created with hex encoding prefixed with a
 * dollar, e.g. foo$5Fbar is foo_bar.
 */
public class RESTResponse extends Exception {
	static final long		serialVersionUID	= 1L;

	private final int		statusCode;
	private final Object	value;
	private final String	contentType;

	/**
	 * Create an OK response with no value and default content type
	 */
	public RESTResponse() {
		this(null, 200, null, null);
	}

	/**
	 * Create a given status response with a message
	 * 
	 * @param message
	 *            the message of the response
	 * @param statusCode
	 *            the status code
	 */
	public RESTResponse(String message, int statusCode) {
		this(message, statusCode, null, null);
	}

	/**
	 * Create a given status response with a message
	 * 
	 * @param statusCode
	 *            the status code
	 * @param value
	 *            the value
	 */
	public RESTResponse(int statusCode, Object value) {
		this(null, statusCode, value, null);
	}

	/**
	 * Create a 200 status response with a value
	 * 
	 * @param value
	 *            the value
	 */
	public RESTResponse(Object value) {
		this(200, value);
	}

	/**
	 * Create a given status response
	 * 
	 * @param statusCode
	 *            the status code
	 */
	public RESTResponse(int statusCode) {
		this(null, statusCode, null, null);
	}

	/**
	 * Create a given status response with a content and content type
	 * 
	 * @param statusCode
	 *            the status code
	 * @param value
	 *            the value
	 * @param contentType
	 *            the content type
	 */
	public RESTResponse(int statusCode, Object value, String contentType) {
		this(null, statusCode, value, contentType);
	}

	/**
	 * Create a given status response with a message and an error
	 * 
	 * @param message
	 *            the message
	 * @param statusCode
	 *            the status code
	 * @param cause
	 *            the cause of the error
	 */
	public RESTResponse(String message, int statusCode, Throwable cause) {
		this(message, statusCode, null, null, cause);
	}

	/**
	 * Create a given status response with a message and an error
	 * 
	 * @param message
	 *            the message
	 * @param statusCode
	 *            the status code
	 * @param cause
	 *            the cause of the error
	 * @param contentType
	 *            the content type
	 * @param value
	 *            the value
	 */
	public RESTResponse(String message, int statusCode, Object value,
			String contentType, Throwable cause) {
		super(message == null ? statusCode + "" : message, cause);
		this.statusCode = statusCode;
		this.value = value;
		this.contentType = contentType;
	}

	/**
	 * Create a given status response with a message, value and content type
	 * 
	 * @param message
	 *            the message
	 * @param statusCode
	 *            the status code
	 * @param contentType
	 *            the content type
	 * @param value
	 *            the value
	 */
	public RESTResponse(String message, int statusCode, Object value,
			String contentType) {
		super(message == null ? statusCode + "" : message);
		this.statusCode = statusCode;
		this.value = value;
		this.contentType = contentType;

	}

	/**
	 * Answer the status code
	 * 
	 * @return the status code
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * Answer the value
	 * 
	 * @return thevalue
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Answer the content type
	 * 
	 * @return the content type
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Add public fields in the subclass as headers.
	 */

	// ...

	/**
	 * Redirect
	 *
	 */

	public static class Redirect extends RESTResponse {
		private static final long serialVersionUID = 1L;
		/**
		 * The redirected location header
		 */
		public URI					Location;

		/**
		 * @param code
		 *            the redirect code (in the 3xx range)
		 * @param uri
		 *            the uri to redirect to
		 */
		public Redirect(int code, URI uri) {
			super(code);
			assert code /100 == 3;
			this.Location = uri;
		}

	}

	/**
	 * Teapot
	 *
	 */

	public static class IamATeapot extends RESTResponse {
		private static final long serialVersionUID = 1L;

		/**
		 */
		public IamATeapot() {
			super("I am a Teapot!", 418);
		}
	}

}
