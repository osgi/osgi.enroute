package osgi.enroute.rest.simple.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.lib.collections.ExtList;
import junit.framework.TestCase;
import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RESTRequest;

/*
 * 
 * 
 * 
 */

public class MapperTest extends TestCase {

	static class WithoutRequest___ implements REST {
		public String getFoo() {
			return "foo";
		}

		public String getFoo(String seg) {
			assertEquals("foo-seg", "hello", seg);
			return "foo-seg";
		}

		public String getFoo(String... varargs) {
			for (String v : varargs) {
				assertEquals("foo-varargs", "hello", v);
			}
			return "foo-varargs/" + varargs.length;
		}

		public String putBar(int body) {
			assertEquals("bar-body", 42, body);
			return "bar-body";
		}

		public String putBar(int body, String seg) {
			assertEquals("bar-body-seg", 42, body);
			assertEquals("bar-body-seg", "hello", seg);

			return "bar-body-seg";
		}

		public String putBar(int body, String... varargs) {
			assertEquals("bar-body-varargs", 42, body);
			for (String v : varargs) {
				assertEquals("foo-varargs", "hello", v);
			}

			return "bar-body-varargs/" + varargs.length;
		}
	}

	static class WithRequest______ implements REST {
		public String getFoo(RESTRequest rq) {
			assertNotNull("foo-rq", rq);
			return "foo-rq";
		}

		public String getFoo(RESTRequest rq, String seg) {
			assertNotNull("foo-rq-seg", rq);
			assertEquals("foo-rq-seg", "hello", seg);
			return "foo-rq-seg";
		}

		public String getFoo(RESTRequest rq, String... varargs) {
			assertNotNull("foo-rq-varargs", rq);
			for (String v : varargs) {
				assertEquals("foo-rq-varargs", "hello", v);
			}
			return "foo-rq-varargs/" + varargs.length;
		}

		public String putBar(RESTRequest rq, int body) {
			assertNotNull("bar-rq-body", rq);
			assertEquals("bar-rq-body", 42, body);

			return "bar-rq-body";
		}

		public String putBar(RESTRequest rq, int body, String seg) {
			assertNotNull("bar-rq-body-seg", rq);
			assertEquals("bar-rq-body-seg", 42, body);
			assertEquals("bar-rq-body-seg", "hello", seg);

			return "bar-rq-body-seg";
		}

		public String putBar(RESTRequest rq, int body, String... varargs) {
			assertNotNull("bar-rq-body-varargs", rq);
			assertEquals("bar-rq-body-varargs", 42, body);
			for (String v : varargs) {
				assertEquals("foo-rq-body-varargs", "hello", v);
			}

			return "bar-rq-body-varargs/" + varargs.length;
		}
	}

	interface ExtraRest extends RESTRequest {
		int _body();
	}

	static class WithBodyRequest__ implements REST {
		public String putBar(ExtraRest rq) {
			assertNotNull("bar-ex", rq);
			assertEquals("bar-ex", 42, rq._body());
			return "bar-ex";
		}

		public String putBar(ExtraRest rq, String seg) {
			assertNotNull("bar-ex-seg", rq);
			assertEquals("bar-ex-seg", "hello", seg);
			assertEquals("bar-ex-seg", 42, rq._body());

			return "bar-ex-seg";
		}

		public String putBar(ExtraRest rq, String... varargs) {
			assertNotNull("bar-ex-varargs", rq);
			assertEquals("bar-ex-varargs", 42, rq._body());
			for (String v : varargs) {
				assertEquals("bar-ex-varargs", "hello", v);
			}

			return "bar-ex-varargs/" + varargs.length;
		}

	}

	static class Varargs implements REST {
		public String getFoo(String... varargs) {
			for (String v : varargs) {
				assertEquals("bar-ex-varargs", "hello", v);
			}

			return "foo-varargs/" + varargs.length;
		}

		public String getFoorq(RESTRequest rq, String... varargs) {
			assertNotNull("foo-rq-varargs", rq);
			for (String v : varargs) {
				assertEquals("foo-rq-varargs", "hello", v);
			}

			return "foo-rq-varargs/" + varargs.length;
		}

		public String putBar_body(int body, String... varargs) {
			assertEquals("bar-body-varargs", 42, body);
			for (String v : varargs) {
				assertEquals("foo-rq-varargs", "hello", v);
			}

			return "bar-body-varargs/" + varargs.length;
		}

		public String putBar_ex(ExtraRest body, String... varargs) {
			assertEquals("bar-ex-varargs", 42, body._body());
			for (String v : varargs) {
				assertEquals("bar-ex-varargs", "hello", v);
			}

			return "bar-ex-varargs/" + varargs.length;
		}

		public String putBar_rq_body(RESTRequest rq, int body, String... varargs) {
			assertEquals("bar-rq-body-varargs", 42, body);
			for (String v : varargs) {
				assertEquals("bar-rq-body-varargs", "hello", v);
			}

			return "bar-rq-body-varargs/" + varargs.length;
		}
	}

	public void testMapper() throws Exception {

		assertMapper(Varargs.class, "getfoo", null, "foo-varargs/0");
		assertMapper(Varargs.class, "getfoorq", null, "foo-rq-varargs/0");

		assertMapper(Varargs.class, "getfoo", "hello", "foo-varargs/1");
		assertMapper(Varargs.class, "getfoorq", "hello", "foo-rq-varargs/1");

		assertMapper(Varargs.class, "getfoo", "hello/hello/hello", "foo-varargs/3");
		assertMapper(Varargs.class, "getfoorq", "hello/hello/hello", "foo-rq-varargs/3");

		assertMapper(Varargs.class, "putbar_body", null, "bar-body-varargs/0");
		assertMapper(Varargs.class, "putbar_ex", null, "bar-ex-varargs/0");
		assertMapper(Varargs.class, "putbar_rq_body", null, "bar-rq-body-varargs/0");

		assertMapper(Varargs.class, "putbar_body", "hello", "bar-body-varargs/1");
		assertMapper(Varargs.class, "putbar_ex", "hello", "bar-ex-varargs/1");
		assertMapper(Varargs.class, "putbar_rq_body", "hello", "bar-rq-body-varargs/1");

		assertMapper(Varargs.class, "putbar_body", "hello/hello/hello/hello", "bar-body-varargs/4");
		assertMapper(Varargs.class, "putbar_ex", "hello/hello/hello/hello", "bar-ex-varargs/4");
		assertMapper(Varargs.class, "putbar_rq_body", "hello/hello/hello/hello", "bar-rq-body-varargs/4");

		assertMapper(WithoutRequest___.class, "putbar", null, "bar-body");
		assertMapper(WithRequest______.class, "putbar", null, "bar-rq-body");
		assertMapper(WithBodyRequest__.class, "putbar", null, "bar-ex");

		assertMapper(WithoutRequest___.class, "putbar", "hello", "bar-body-seg");
		assertMapper(WithRequest______.class, "putbar", "hello", "bar-rq-body-seg");
		assertMapper(WithBodyRequest__.class, "putbar", "hello", "bar-ex-seg");

		assertMapper(WithoutRequest___.class, "putbar", "hello/hello", "bar-body-varargs/2");
		assertMapper(WithRequest______.class, "putbar", "hello/hello", "bar-rq-body-varargs/2");
		assertMapper(WithBodyRequest__.class, "putbar", "hello/hello", "bar-ex-varargs/2");

		assertMapper(WithoutRequest___.class, "putbar", "hello/hello/hello", "bar-body-varargs/3");
		assertMapper(WithRequest______.class, "putbar", "hello/hello/hello", "bar-rq-body-varargs/3");
		assertMapper(WithBodyRequest__.class, "putbar", "hello/hello/hello", "bar-ex-varargs/3");

		assertMapper(WithoutRequest___.class, "getfoo", null, "foo");
		assertMapper(WithRequest______.class, "getfoo", null, "foo-rq");

		assertMapper(WithoutRequest___.class, "getfoo", "hello", "foo-seg");
		assertMapper(WithRequest______.class, "getfoo", "hello", "foo-rq-seg");

		assertMapper(WithoutRequest___.class, "getfoo", "hello/hello", "foo-varargs/2");
		assertMapper(WithRequest______.class, "getfoo", "hello/hello", "foo-rq-varargs/2");

		assertMapper(WithoutRequest___.class, "getfoo", "hello/hello/hello", "foo-varargs/3");
		assertMapper(WithRequest______.class, "getfoo", "hello/hello/hello", "foo-rq-varargs/3");
	}

	private void assertMapper(Class<? extends REST> clazz, String method, String parameters, String result)
			throws Exception {
		
		method = RestMapper.decode(method);
		String parms[] = parameters == null ? new String[0] : parameters.split("/");
		ExtList<String> ps = new ExtList<>(parms);

		RestMapper mapper = new RestMapper("/rest");
		mapper.addResource(clazz.newInstance(), 100);

		String cardinality = method + "/" + ps.size();
		List<Function> fs = mapper.functions.get(cardinality);
		if (fs == null)
			fs = mapper.functions.get(method);

		assertNotNull(clazz + " " + method + "/" + ps.size(), fs);
		assertEquals(method + "/" + ps.size(), 1, fs.size());
		Function f = fs.get(0);
		Map<String, Object> args = new HashMap<>();
		args.put("_body", 42);
		Object[] match = f.match(args, ps);
		if (f.hasPayloadAsParameter)
			match[f.hasRequestParameter ? 1 : 0] = 42;

		Object invoke = f.invoke(match);
		assertEquals(result, invoke);

	}

}
