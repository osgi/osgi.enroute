package osgi.enroute.jsonrpc.dto;

import java.util.ArrayList;
import java.util.List;

import org.osgi.dto.DTO;

/**
 * The messages used in the JSON RPC protocol
 */
public interface JSON {
	/**
	 * Defines a JSON RPC request
	 */
	public static class Request extends DTO {
		/**
		 * The protocol version
		 */
		public String		jsonrpc	= "2.0";
		/**
		 * The method to invoke
		 */
		public String		method;
		/**
		 * The parameters of the invocation
		 */
		public List<Object>	params	= new ArrayList<>();

		/**
		 * The transaction id
		 */
		public long id;
	}

	/**
	 * Defines the Response type of a JSON RPC request
	 */
	public static class Response extends DTO {
		/**
		 * The protocol version
		 */
		public String		jsonrpc	= "2.0";
		/**
		 * The return result
		 */
		public Object		result;
		/**
		 * Error information
		 */
		public JSONRPCError	error;
		/**
		 * The transaction id
		 */
		public long			id;
	}

	/**
	 * The JSON RPC Error information
	 */
	public static class JSONRPCError extends DTO {
		/**
		 * The error code
		 */
		public long			code;
		/**
		 * A message for the error
		 */
		public String		message;
		/**
		 * An optional stack trace
		 */
		public List<String>	trace;
	}

	/**
	 * An endpoint descriptor. This is returned to the caller at first
	 * handshake.
	 */
	public static class Endpoint extends DTO {
		/**
		 * The name of the endpoint
		 */
		public String		name;
		/**
		 * The methods defined on the endpoint
		 */
		public List<String>	methods	= new ArrayList<>();
		/**
		 * The custom descriptor from the endpoint
		 */
		public Object		descriptor;
	}

}
