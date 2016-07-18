package osgi.enroute.rest.simple.test;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import aQute.bnd.annotation.component.Reference;
import aQute.bnd.testing.DSTestWiring;
import aQute.lib.io.IO;
import aQute.libg.map.MAP;
import junit.framework.TestCase;
import osgi.enroute.configurer.api.ConfigurationDone;
import osgi.enroute.configurer.api.RequireConfigurerExtender;
import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RESTRequest;
import osgi.enroute.rest.api.RequireRestImplementation;
import osgi.enroute.rest.api.UriMapper;

@RequireConfigurerExtender
@RequireRestImplementation
public class RestNamespaceTest extends TestCase {

	BundleContext	context	= FrameworkUtil.getBundle(RestNamespaceTest.class).getBundleContext();
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

	public void testDifferentServletsDifferentNamespaces() throws Exception {
        ServiceRegistration<REST> defaultREST = defaultREST();
        ServiceRegistration<REST> ns1REST = ns1REST();
        ServiceRegistration<REST> ns2REST = ns2REST();
        ServiceRegistration<UriMapper> mapper1 = mapper("/rest1/*", "ns1");
        ServiceRegistration<UriMapper> mapper2 = mapper("/rest2/*", "ns2");

		try {
            // Always test the default
			URL url = new URL("http://localhost:8080/rest/namespace/arg1");
			String s = IO.collect(url.openStream());
			assertEquals("\"::arg1\"", s);

            URL url2 = new URL("http://localhost:8080/rest1/namespace1/arg1");
            String s2 = IO.collect(url2.openStream());
            assertEquals("\"ns1::arg1\"", s2);

            URL url3 = new URL("http://localhost:8080/rest2/namespace2/arg1");
            String s3 = IO.collect(url3.openStream());
            assertEquals("\"ns2::arg1\"", s3);
		} finally {
            mapper2.unregister();
            mapper1.unregister();
            ns2REST.unregister();
            ns1REST.unregister();
			defaultREST.unregister();
		}
	}

    public void testSameServletDifferentNamespaces() throws Exception {
        ServiceRegistration<REST> defaultREST = defaultREST();
        ServiceRegistration<REST> ns1REST = ns1REST();
        ServiceRegistration<REST> ns2REST = ns2REST();
        ServiceRegistration<UriMapper> multiMapper = multiMapper("/rest/*", "ns1", "ns2");

        try {
            // Always test the default
            URL url = new URL("http://localhost:8080/rest/namespace/arg1");
            String s = IO.collect(url.openStream());
            assertEquals("\"::arg1\"", s);

            URL url2 = new URL("http://localhost:8080/rest/namespace1/arg1");
            String s2 = IO.collect(url2.openStream());
            assertEquals("\"ns1::arg1\"", s2);

            URL url3 = new URL("http://localhost:8080/rest/namespace2/arg1");
            String s3 = IO.collect(url3.openStream());
            assertEquals("\"ns2::arg1\"", s3);
        } finally {
            multiMapper.unregister();
            ns2REST.unregister();
            ns1REST.unregister();
            defaultREST.unregister();
        }
    }

    public void testDifferentServletsSameNamespace() throws Exception {
        ServiceRegistration<REST> defaultREST = defaultREST();
        ServiceRegistration<REST> ns1REST = ns1REST();
        ServiceRegistration<REST> ns1REST2 = ns1REST2();
        ServiceRegistration<UriMapper> mapper1 = mapper("/rest1/*", "ns1");
        ServiceRegistration<UriMapper> mapper2 = mapper("/rest2/*", "ns1");

        try {
            // Always test the default
            URL url = new URL("http://localhost:8080/rest/namespace/arg1");
            String s = IO.collect(url.openStream());
            assertEquals("\"::arg1\"", s);

            URL url2 = new URL("http://localhost:8080/rest1/namespace1/arg1");
            String s2 = IO.collect(url2.openStream());
            assertEquals("\"ns1::arg1\"", s2);

            URL url3 = new URL("http://localhost:8080/rest2/namespace1/arg1/arg2");
            String s3 = IO.collect(url3.openStream());
            assertEquals("\"ns1::arg1;arg2\"", s3);
        } finally {
            mapper2.unregister();
            mapper1.unregister();
            ns1REST2.unregister();
            ns1REST.unregister();
            defaultREST.unregister();
        }
    }

    public void testSameServletSameNamespace() throws Exception {
        ServiceRegistration<REST> defaultREST = defaultREST();
        ServiceRegistration<REST> ns1REST = ns1REST();
        ServiceRegistration<REST> ns1REST2 = ns1REST2();
        ServiceRegistration<UriMapper> mapper1 = mapper("/rest1/*", "ns1");
        ServiceRegistration<UriMapper> mapper2 = mapper("/rest2/*", "ns1");

        try {
            // Always test the default
            URL url = new URL("http://localhost:8080/rest/namespace/arg1");
            String s = IO.collect(url.openStream());
            assertEquals("\"::arg1\"", s);

            URL url2 = new URL("http://localhost:8080/rest1/namespace1/arg1");
            String s2 = IO.collect(url2.openStream());
            assertEquals("\"ns1::arg1\"", s2);

            URL url3 = new URL("http://localhost:8080/rest1/namespace1/arg1/arg2");
            String s3 = IO.collect(url3.openStream());
            assertEquals("\"ns1::arg1;arg2\"", s3);
        } finally {
            mapper2.unregister();
            mapper1.unregister();
            ns1REST2.unregister();
            ns1REST.unregister();
            defaultREST.unregister();
        }
    }

    public static class RestExampleNoNs implements REST {
	    public String getNamespace(RESTRequest rr, String first) {
	        return "::" + first;
	    }
	}

    public static class RestExampleNs1 implements REST {
        public String getNamespace1(RESTRequest rr, String first) {
            return "ns1::" + first;
        }
    }

    public static class RestExample2Ns1 implements REST {
        public String getNamespace1(RESTRequest rr, String first, String second) {
            return "ns1::" + first + ";" + second;
        }
    }

    public static class RestExampleNs2 implements REST {
        public String getNamespace2(RESTRequest rr, String first) {
            return "ns2::" + first;
        }
    }

    public static class UriMapperExample implements UriMapper {
        private final String mapTo;

        public UriMapperExample(String namespace) {
            mapTo = namespace;
        }

        @Override
        public String namespaceFor( String uri ) {
            return mapTo;
        }
    }

    public static class UriMultiMapperExample implements UriMapper {
        private final String mapTo1, mapTo2;

        public UriMultiMapperExample(String namespace1, String namespace2) {
            mapTo1 = namespace1;
            mapTo2 = namespace2;
        }

        @Override
        public String namespaceFor( String uri ) {
            if(uri.contains("namespace1" ))
                return mapTo1;
            if(uri.contains("namespace2" ))
                return mapTo2;
            return null;
        }
    }

    private ServiceRegistration<REST> defaultREST() {
         return context.registerService(
                        REST.class,
                        new RestExampleNoNs(), 
                        MAP.$(Constants.SERVICE_RANKING, 100).asHashtable());
    }

    private ServiceRegistration<REST> ns1REST() {
        Dictionary<String, Object> d = new Hashtable<>();
        d.put(Constants.SERVICE_RANKING, 100);
        d.put("org.enroute.rest.namespace", "ns1");
        return context.registerService(REST.class, new RestExampleNs1(), d);
    }

    private ServiceRegistration<REST> ns1REST2() {
        Dictionary<String, Object> d = new Hashtable<>();
        d.put(Constants.SERVICE_RANKING, 100);
        d.put("org.enroute.rest.namespace", "ns1");
        return context.registerService(REST.class, new RestExample2Ns1(), d);
    }

    private ServiceRegistration<REST> ns2REST() {
        Dictionary<String, Object> d = new Hashtable<>();
        d.put(Constants.SERVICE_RANKING, 100);
        d.put("org.enroute.rest.namespace", "ns2");
        return context.registerService(REST.class, new RestExampleNs2(), d);
    }

    private ServiceRegistration<UriMapper> mapper(String servletPattern, String namespace) {
        Dictionary<String, Object> d = new Hashtable<>();
        d.put(Constants.SERVICE_PID, "org.enroute.rest.mapper");
        d.put(Constants.SERVICE_RANKING, 100);
        d.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, servletPattern);
        return context.registerService(UriMapper.class, new UriMapperExample(namespace), d);
    }

    private ServiceRegistration<UriMapper> multiMapper(String servletPattern, String namespace1, String namespace2) {
        Dictionary<String, Object> d = new Hashtable<>();
        d.put(Constants.SERVICE_PID, "org.enroute.rest.mapper");
        d.put(Constants.SERVICE_RANKING, 100);
        d.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, servletPattern);
        return context.registerService(UriMapper.class, new UriMultiMapperExample(namespace1, namespace2), d);
    }

    @Reference
	void setConfigurationDone( ConfigurationDone d) {
		System.out.println("Configuration Done");
	}
}
