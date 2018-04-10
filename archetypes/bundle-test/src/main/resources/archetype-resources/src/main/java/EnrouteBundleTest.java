#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * This is a JUnit test that will be run inside an OSGi framework.
 * 
 * It can interact with the framework by starting or stopping bundles,
 * getting or registering services, or in other ways, and then observing
 * the result on the bundle(s) being tested.
 */
public class EnrouteBundleTest {
    
    private final Bundle bundle = FrameworkUtil.getBundle(this.getClass());
    
    @Before
    public void setUp() throws Exception {
        assertNotNull("OSGi Bundle tests must be run inside an OSGi framework", bundle);
    }
    
    @After
    public void tearDown() throws Exception {
        // TODO clean up any changes made
    }
    
    @Test
    public void testOSGiBundle() throws Exception {
        // TODO look at the bundle under test
    }
}
