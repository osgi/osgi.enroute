package osgi.enroute.jsonrpc.api;

/**
 * Marker interface, will call methods on this interface from JSON. That means
 * that all methods in the implementation class must protect themselves agains
 * the outside world.
 */

public interface JSONRPC {
	/**
	 * A service property to indicate the endpoint name of the JSON RPC
	 * protocol. This name should be unique and should contain a version
	 * identifier. It must consist of characters that are URL friendly. The best
	 * is to use the symbol type from the OSGi specifications.
	 */
	String ENDPOINT = "endpoint";

	/**
	 * Return information that could be needed by clients of this endpoint or
	 * null if no such info exists. This information is per connection so it is
	 * possible to customize for the current user.
	 * 
	 * @return The custom descriptor. Should contain DTO objects
	 * @throws Exception
	 */
	Object getDescriptor() throws Exception;
}
