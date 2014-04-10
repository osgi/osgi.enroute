package osgi.enroute.base.test;

import junit.framework.TestCase;

import org.osgi.framework.FrameworkUtil;

public class TestTest extends TestCase {

	private int before;
	
	public void setUp() {
		before = 1;
	}
	public void testSimple() {
		assertEquals(1,before);
		assertNotNull(FrameworkUtil.getBundle(TestTest.class));
	}
}
