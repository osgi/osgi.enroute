package osgi.enroute.jsonrpc.api;

/**
 * Marker interface, will call methods on this interface from JSON. That means
 * that all methods in the implementation class must protect themselves agains
 * the outside world.
 */

public interface JSONRPC {
	String	ENDPOINT	= "endpoint";

	/**
	 * Return information that could be needed by clients of this endpoint or
	 * null if no such info exists. This information is per connection so it is
	 * possible to customize for the current user.
	 * 
	 * @return
	 * @throws Exception
	 */
	Object getDescriptor() throws Exception;
}
