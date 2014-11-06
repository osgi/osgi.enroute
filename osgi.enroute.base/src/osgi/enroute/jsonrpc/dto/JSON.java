package osgi.enroute.jsonrpc.dto;

import java.util.ArrayList;
import java.util.List;

import org.osgi.dto.DTO;

/**
 * The messages used in the JSON RPC protocol
 */
public interface JSON {
	public static class Request extends DTO {
		public String		jsonrpc	= "2.0";
		public String		method;
		public List<Object>	params	= new ArrayList<>();
		public long			id;
	}

	public static class Response extends DTO {
		public String		jsonrpc	= "2.0";
		public Object		result;
		public JSONRPCError	error;
		public long			id;
	}

	public static class JSONRPCError extends DTO {
		public long			code;
		public String		message;
		public List<String>	trace;
	}
	
	public static class Endpoint extends DTO {
		public String		name;
		public List<String>	methods	= new ArrayList<>();
		public Object		descriptor;
	}

}
