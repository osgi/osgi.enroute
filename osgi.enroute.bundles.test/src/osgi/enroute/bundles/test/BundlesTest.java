package osgi.enroute.bundles.test;

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

public class BundlesTest {

    private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    
    /*
     * 
     * 
     * 
     * 
     */
    @Test
    public void testBundles() throws Exception {
    	Assert.assertNotNull(context);
    }
}
