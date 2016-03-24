package osgi.enroute.web.simple.test;

import javax.servlet.http.HttpServlet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.testing.DSTestWiring;
import junit.framework.TestCase;

public class DispatcherTest extends TestCase {

    BundleContext           context = FrameworkUtil.getBundle(WebTests.class).getBundleContext();
    DSTestWiring            ds      = new DSTestWiring();
    private HttpServlet     servlet;

    public void setUp() throws Exception {
        ds.setContext(context);
        ds.add(this);
        ds.wire();
    }

    public void testThis() throws Exception {
        assertNotNull(servlet);
    }

    @Reference(target="(name=DispatchServlet)")
    void setDispatchServlet(HttpServlet servlet) {
        this.servlet = servlet;
    }
}
