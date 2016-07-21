package osgi.enroute.rest.simple.test;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.dto.DTO;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.annotation.component.Reference;
import aQute.bnd.testing.DSTestWiring;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.libg.map.MAP;
import junit.framework.TestCase;
import osgi.enroute.configurer.api.ConfigurationDone;
import osgi.enroute.configurer.api.RequireConfigurerExtender;
import osgi.enroute.dto.api.DTOs;
import osgi.enroute.rest.api.Inf;
import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RESTRequest;
import osgi.enroute.rest.api.RequireRestImplementation;

@RequireConfigurerExtender
@RequireRestImplementation
public class RestDefaultTest extends TestCase {

	BundleContext	context	= FrameworkUtil.getBundle(RestDefaultTest.class).getBundleContext();
	DSTestWiring	ds		= new DSTestWiring();

	
	public void setUp() throws Exception {
		ds.setContext(context);
		ds.add(this);
		ds.wire();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testNoArgs() throws Exception {
		ServiceRegistration<REST> rest = 
		        context.registerService(
		                REST.class,
		                new RestExample(), 
		                MAP.$(Constants.SERVICE_RANKING, 100).asHashtable());

		try {
			URL url = new URL("http://localhost:8080/rest/upper/");
			String s = IO.collect(url.openStream());
			assertEquals("\"No Parameters\"", s);
		} finally {
			rest.unregister();
		}
	}

    public void testOneArg() throws Exception {
        ServiceRegistration<REST> rest = 
                context.registerService(
                        REST.class,
                        new RestExample(), 
                        MAP.$(Constants.SERVICE_RANKING, 100).asHashtable());

        try {
            URL url = new URL("http://localhost:8080/rest/upper/arg1");
            String s = IO.collect(url.openStream());
            assertEquals("\"ARG1\"", s);
        } finally {
            rest.unregister();
        }
    }

    public void testSixArgs() throws Exception {
        ServiceRegistration<REST> rest = 
                context.registerService(
                        REST.class,
                        new RestExample(), 
                        MAP.$(Constants.SERVICE_RANKING, 100).asHashtable());

        try {
            URL url = new URL("http://localhost:8080/rest/upper/arg1/arg2/arg3/arg4/arg5/arg6");
            String s = IO.collect(url.openStream());
            assertEquals("\"ARG1&ARG2&ARG3&ARG4&ARG5&ARG6\"", s);
        } finally {
            rest.unregister();
        }
    }

    public void testOneParam() throws Exception {
        ServiceRegistration<REST> rest = 
                context.registerService(
                        REST.class,
                        new RestExample(), 
                        MAP.$(Constants.SERVICE_RANKING, 100).asHashtable());

        try {
            URL url = new URL("http://localhost:8080/rest/upper2?param1=1");
            String s = IO.collect(url.openStream());
            assertEquals("\"1\"", s);
        } finally {
            rest.unregister();
        }
    }

    public void testSixParams() throws Exception {
        ServiceRegistration<REST> rest = 
                context.registerService(
                        REST.class,
                        new RestExample(), 
                        MAP.$(Constants.SERVICE_RANKING, 100).asHashtable());

        try {
            URL url = new URL("http://localhost:8080/rest/upper2?param6=6&param3=3");
            String s = IO.collect(url.openStream());
            assertEquals("\"3&6\"", s);
        } finally {
            rest.unregister();
        }
    }

    public void testSerializedObject() throws Exception {
        ServiceRegistration<REST> rest = 
                context.registerService(
                        REST.class,
                        new RestExample(), 
                        MAP.$(Constants.SERVICE_RANKING, 100).asHashtable());

        try {
            URL url1 = new URL("http://localhost:8080/rest/upper3/TesT");
            IO.collect(url1.openStream());
            URL url2 = new URL("http://localhost:8080/rest/upper3/TesT2");
            String s2 = IO.collect(url2.openStream());
            assertEquals("{\"input\":\"TesT2\",\"output\":\"TEST2\"}", s2);
        } finally {
            rest.unregister();
        }
    }

    // Test works when run individually.
    // Problem with test case? Or some kind of race condition in the code??
    public void _ignore_testDelete() throws Exception {
        RestExample example = new RestExample();
        ServiceRegistration<REST> rest = 
                context.registerService(
                        REST.class,
                        example, 
                        MAP.$(Constants.SERVICE_RANKING, 100).asHashtable());

        try {
            // Set up the data
            URL url1 = new URL("http://localhost:8080/rest/upper3/TesT");
            IO.collect(url1.openStream());
            URL url2 = new URL("http://localhost:8080/rest/upper3/TesT2");
            IO.collect(url2.openStream());
            URL url3 = new URL("http://localhost:8080/rest/upper3/TesT3");
            IO.collect(url3.openStream());

            assertEquals(3, example.history.size());
            assertTrue(example.history.containsKey("TesT"));
            assertTrue(example.history.containsKey("TesT2"));
            assertTrue(example.history.containsKey("TesT3"));

            // Delete
            URL url4 = new URL("http://localhost:8080/rest/upper3/TesT2");
            HttpURLConnection httpCon = (HttpURLConnection)url4.openConnection();
            httpCon.setDoOutput(true);
            httpCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpCon.setRequestMethod("DELETE");
            httpCon.connect();
            String s4 = IO.collect(httpCon.getInputStream());

            assertEquals(2, example.history.size());
            assertTrue(example.history.containsKey("TesT"));            
            assertEquals("{\"TesT\":{\"input\":\"TesT\",\"output\":\"TEST\"},\"TesT3\":{\"input\":\"TesT3\",\"output\":\"TEST3\"}}", s4);
        } finally {
            rest.unregister();
        }
    }

    public void testPostNoPayload() throws Exception {
        RestExample example = new RestExample();
        ServiceRegistration<REST> rest = 
                context.registerService(
                        REST.class,
                        example, 
                        MAP.$(Constants.SERVICE_RANKING, 100).asHashtable());

        try {
            // Post
            String s = post(new URL("http://localhost:8080/rest/upper4/TesT"),null);
            assertEquals("{\"input\":\"TesT\",\"output\":\"TEST\"}", s);
        } finally {
            rest.unregister();
        }
    }

    public void testPutNoPayload() throws Exception {
        RestExample example = new RestExample();
        ServiceRegistration<REST> rest = 
                context.registerService(
                        REST.class,
                        example, 
                        MAP.$(Constants.SERVICE_RANKING, 100).asHashtable());

        try {
            // Post
            String s = put(new URL("http://localhost:8080/rest/upper4/TesT"),null);
            assertEquals("{\"input\":\"TesT\",\"output\":\"TEST\"}", s);
        } finally {
            rest.unregister();
        }
    }

    public void testPostWithPayload() throws Exception {
        RestExample example = new RestExample();
        ServiceRegistration<REST> rest = 
                context.registerService(
                        REST.class,
                        example, 
                        MAP.$(Constants.SERVICE_RANKING, 100).asHashtable());

        try {
            String s = post(new URL("http://localhost:8080/rest/upper5"), new Example("TesT", "TEST"));
            assertEquals("{\"input\":\"TesT\",\"output\":\"TEST\"}", s);
        } finally {
            rest.unregister();
        }
    }

    public void testPutWithPayload() throws Exception {
        RestExample example = new RestExample();
        ServiceRegistration<REST> rest = 
                context.registerService(
                        REST.class,
                        example, 
                        MAP.$(Constants.SERVICE_RANKING, 100).asHashtable());

        try {
            String s = put(new URL("http://localhost:8080/rest/upper5"), new Example("TesT", "TEST"));
            assertEquals("{\"input\":\"TesT\",\"output\":\"TEST\"}", s);
        } finally {
            rest.unregister();
        }
    }

    public void testInf() throws Exception {
        ServiceRegistration<REST> rest = 
                context.registerService(
                        REST.class,
                        new RestExample(), 
                        MAP.$(Constants.SERVICE_RANKING, 100).asHashtable());

        try {
            URL url = new URL("http://localhost:8080/rest/inf");
            String s = IO.collect(url.openStream());
            System.out.print(s);
        } finally {
            rest.unregister();
        }
    }

    public static class RestExample implements REST {

	    //*************************************
	    //GET Examples using URL arguments
	    //*************************************
	    
	    //GET http://localhost:8080/rest/upper/
        @Inf("An example GET call with no arguments")
	    public String getUpper(RESTRequest rr) {
	        return "No Parameters";
	    }
	    
	    //GET http://localhost:8080/rest/upper/arg1
        @Inf("An example GET call with one argument")
	    public String getUpper(RESTRequest rr, @Inf("First argument") String first) {
	        return first.toUpperCase();
	    }
	    
	    //GET http://localhost:8080/rest/upper/arg1/arg2/arg3/arg4/arg5/arg6
        @Inf("An example GET call with six arguments")
	    public String getUpper(
	            RESTRequest rr, 
	            @Inf("First argument") String arg1, 
	            @Inf("Second argument") String arg2, 
	            @Inf("Third argument") String arg3, 
	            @Inf("Fourth argument") String arg4, 
	            @Inf("Fifth argument") String arg5, 
	            @Inf("Sixth argument") String arg6) {
	        return 
	                arg1.toUpperCase() + "&" + 
	                arg2.toUpperCase() + "&" +
	                arg3.toUpperCase() + "&" +
	                arg4.toUpperCase() + "&" +
	                arg5.toUpperCase() + "&" +
	                arg6.toUpperCase();
	    }
	    
	    //*************************************
	    //GET Example using URL parameters
	    //*************************************
	    
	    //note the name of the member variables map to the URL parameters
	    interface UpperRequest2 extends RESTRequest {
	        @Inf("First parameter") String param1();
	        @Inf("Second parameter") String param2();
	        @Inf("Third parameter") String param3();
	        @Inf("Fourth parameter") String param4();
	        @Inf("Fifth parameter") String param5();
	        @Inf("Sixth parameter") String param6();
	    }  

	    //GET http://localhost:8080/rest/upper2/?param1=x1&param2=x2...
        @Inf("An example GET call with six parameters")
	    public String getUpper2(UpperRequest2 ur) {
	        StringBuilder b = new StringBuilder();
	        if(ur.param1() != null) {
	            b.append(ur.param1());
	            if(notNull(ur.param2(), ur.param3(), ur.param4(), ur.param5(), ur.param6()))
	                b.append("&");
	        }

            if(ur.param2() != null) {
                b.append(ur.param2());
                if(notNull(ur.param3(), ur.param4(), ur.param5(), ur.param6()))
                    b.append("&");
            }

            if(ur.param3() != null) {
                b.append(ur.param3());
                if(notNull(ur.param4(), ur.param5(), ur.param6()))
                    b.append("&");
            }

            if(ur.param4() != null) {
                b.append(ur.param4());
                if(notNull(ur.param5(), ur.param6()))
                    b.append("&");
            }

            if(ur.param5() != null) {
                b.append(ur.param5());
                if(notNull(ur.param6()))
                    b.append("&");
            }

            if(ur.param6() != null)
                b.append(ur.param6());

            return b.toString();
	    }

	    //*************************************
	    //GET Example returning a serialized object
	    //DELETE Example using URL arguments
	    //*************************************

	    public static class History extends DTO {
	        public String input;
	        public String output;
	    }

	    public static class Customer
	        extends DTO
	    {
	        public String fn;
	        public String ln;
	    }

	    private final Map<String, History> history = new ConcurrentHashMap<>();

	    //GET http://localhost:8080/rest/upper3/TesT ==> returns {"input":"TesT","output":"TEST"}
	    //GET http://localhost:8080/rest/upper3/TesT2 ==> returns {"input":"TesT2","output":"TEST2"}
        @Inf("An example GET call with one argument")
	    public History getUpper3(RESTRequest rr, String string) {
	        History h = new History();
	        h.input = string;
	        h.output = string.toUpperCase();
	        history.put(h.input, h);
	        return h;
	    }

	    //DELETE http://localhost:8080/rest/upper3/TesT2
        @Inf("An example DELETE call with one argument")
	    public Map<String, History> deleteUpper3(RESTRequest rr, @Inf("First argument") String string) {
	        history.remove(string);
	        return history;
	    }

	    //*************************************
	    //POST Example - no payload
	    //*************************************
	    
	    //POST http://localhost:8080/rest/upper4/TesT
        @Inf("An example POST call with payload as argument and one string argument")
	    public History postUpper4(RESTRequest rr, String payload, @Inf("First argument") String string) {
	        History h = new History();
	        h.input = string;
	        h.output = string.toUpperCase();
	        history.put(h.input, h);
	        return h;
	    }

        //*************************************
        //PUT Example
        //*************************************
        
	    //PUT http://localhost:8080/rest/upper4/TesT
	    //PUT http://localhost:8080/rest/upper4/TesT2
        @Inf("An example PUT call with payload as argument and one string argument")
	    public History putUpper4(RESTRequest rr, String payload, @Inf("First argument") String string) {
	        History h = new History();
	        h.input = string;
	        h.output = string.toUpperCase();
	        history.put(h.input, h);
	        return h;
	    }

	    //*************************************
	    //POST and PUT Examples - using payload
	    //*************************************
	    interface UpperRequest5 extends RESTRequest {
	        History _body();
	    }

	    //POST http://localhost:8080/rest/upper5/ with a payload
        @Inf("An example POST call with a payload and no arguments")
	    public History postUpper5(UpperRequest5 rq5) {
	        History h = rq5._body();
            history.put(h.input, h);
	        return h;
	    }
	    
	    //PUT http://localhost:8080/rest/upper5/ with a payload
        @Inf("An example PUT call with a payload and no arguments")
	    public History putUpper5(UpperRequest5 rq5) {
	        History h = rq5._body();
	        history.put(h.input, h);
	        return h;
	    }

	    private boolean notNull( String... args )
	    {
	        for(String arg : args)
	            if( arg != null )
	                return true;

	        return false;
	    }
	}

    public static class Example extends DTO {
        public String input;
        public String output;

        Example(String input, String output) {
            this.input = input;
            this.output = output;
        }
    }

    private String post(URL url, DTO payload) throws Exception {
        HttpURLConnection httpCon = null;
        try
        {
            httpCon = (HttpURLConnection)url.openConnection();
            httpCon.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            httpCon.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            httpCon.setRequestMethod("POST");
            if(payload != null) {
                httpCon.setDoOutput(true);
                DataOutputStream dos = new DataOutputStream(httpCon.getOutputStream());
                JSONCodec codec = new JSONCodec();
                codec.enc().charset("UTF-8").to(dos).put(payload);
                dos.close();
            }
            httpCon.connect();
            String s = IO.collect(httpCon.getInputStream());
            return s;
        } catch ( Exception e ) {
            String s = IO.collect(httpCon.getErrorStream());
            System.err.println(s);
            throw e;
        }
    }

    private String put(URL url, DTO payload) throws Exception {
        HttpURLConnection httpCon = null;
        try
        {
            httpCon = (HttpURLConnection)url.openConnection();
            httpCon.setRequestMethod("PUT");
            if(payload != null) {
                httpCon.setDoOutput(true);
                DataOutputStream dos = new DataOutputStream(httpCon.getOutputStream());
                JSONCodec codec = new JSONCodec();
                codec.enc().charset("UTF-8").to(dos).put(payload);
                dos.close();
            }
            httpCon.connect();
            String s = IO.collect(httpCon.getInputStream());
            return s;
        } catch ( Exception e ) {
            String s = IO.collect(httpCon.getErrorStream());
            System.err.println(s);
            throw e;
        }
    }

    DTOs dtos;
    @Reference
    void setDtos(DTOs dtos) {
        this.dtos = dtos;
    }

    @Reference
	void setConfigurationDone( ConfigurationDone d) {
		System.out.println("Configuration Done");
	}
}
