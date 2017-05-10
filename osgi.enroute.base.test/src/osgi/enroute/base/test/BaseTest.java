package osgi.enroute.base.test;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * 
 * 
 * 
 * 
 * 
 * 
 */

public class BaseTest {

    private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    
    /*
     * 
     * 
     * 
     * 
     */
    @Test
    public void testBase() throws Exception {
    	Assert.assertNotNull(context);
    }
}
