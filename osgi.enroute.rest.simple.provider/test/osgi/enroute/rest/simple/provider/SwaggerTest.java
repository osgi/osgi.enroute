package osgi.enroute.rest.simple.provider;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.osgi.dto.DTO;

import aQute.lib.json.JSONCodec;
import junit.framework.TestCase;
import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RESTRequest;
import osgi.enroute.rest.api.RESTResponse;
import osgi.enroute.rest.openapi.annotations.Description;
import osgi.enroute.rest.openapi.annotations.Info;
import osgi.enroute.rest.openapi.annotations.Required;
import osgi.enroute.rest.openapi.annotations.ValidatorArray;
import osgi.enroute.rest.openapi.annotations.ValidatorNumber;
import osgi.enroute.rest.openapi.annotations.ValidatorString;

public class SwaggerTest extends TestCase {

	@Description("Foo class")
	public static class Foo extends DTO {

		@ValidatorString(pattern="abc")
		public String		a;
		@Description("=b")
		public String				b;
		@ValidatorArray(minItems=10)
		public List<@ValidatorString(pattern="XXX")String>	c;
	}

	interface T1 extends RESTRequest {
		Foo _body();

		String p1();

		String[] p2();

		@Required
		int n();
	}

	@Info(title = "EP", description = "info", version = "1.0", contactName="Peter Kriens")
	public static class EP implements REST {

		@Description("operation")
		public  Foo getExample(@ValidatorNumber(minimum=10) int bar)
				throws @Description("filenotfound")
					FileNotFoundException {
			return new Foo();
		}

		static class MyResponse extends RESTResponse {
			private static final long serialVersionUID = 1L;

			
		}
		
		static class MyErrorResponse extends RESTResponse {
			private static final long serialVersionUID = 1L;
			MyErrorResponse(){
				super(509);
			}
		}

		public MyResponse getResponse(int bar)
				throws 
					FileNotFoundException, MyErrorResponse {
			
			return null;
		}

		public Foo getExample(T1 t1) {
			return new Foo();
		}

		public Foo getResponse(T1 t1) throws FileNotFoundException {
			return new Foo();
		}

	}

	public void testSimple() throws URISyntaxException, Exception {
		RestMapper mapper = new RestMapper("/rest");
		mapper.addResource(new EP(), 100);

		OpenAPI openAPI = new OpenAPI(mapper, new URI("http://localhost/rest"));
		
		assertEquals( "1.0", openAPI.info.version);
		assertEquals( "EP", openAPI.info.title);
		assertEquals( "info", openAPI.info.description);
		
		JSONCodec codec = new JSONCodec();
		codec.setIgnorenull(true);
		String string = codec.enc().indent("  ").put(openAPI).toString();
		System.out.println(string);
	}
}
