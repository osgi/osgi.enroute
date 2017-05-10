package osgi.enroute.rest.simple.provider;

import java.io.FileNotFoundException;

import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RESTResponse;
import osgi.enroute.rest.openapi.annotations.Description;
import osgi.enroute.rest.openapi.annotations.Info;

// @formatter: off
@Info(
		title="Example Open API annotations", 
		description="Shows all the Open API annotations in place",
		version="3.0.0.draft"
)
public class OpenAPIExample implements REST {

	
	@Description("My Response")
	static class MyResponse extends RESTResponse {
		private static final long serialVersionUID = 1L;

		MyResponse() {
			super(200, "value", "text/plain");
		}
		
		public MyResponse(int code, String value) {
			super(code, value, "text/plain");
		}

		public int X_RATE_LIMIT;
	}
	
	@Description("My Error Response")
	static class ErrorResponse extends RESTResponse {
		private static final long serialVersionUID = 1L;
	}
	
	/**
	 * Header example
	 */
	@Description("Shows a created response with a header")
	public MyResponse getHeader_ok() {
		
		MyResponse mr = new MyResponse(201,"Boo");
		mr.X_RATE_LIMIT = 10; 
		return mr;
	}
	
	/**
	 * Header example
	 */
	@Description("Alternate response")
	public MyResponse getHeader_ok(int foo) throws ErrorResponse, UnsupportedOperationException {
		
		throw new ErrorResponse();
	}
	
	public String getResponse_ok()
			throws FileNotFoundException, IllegalArgumentException {
		return "";
	}
}
