package osgi.enroute.web.server.provider;

import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

public class SimpleImplTest extends TestCase {

	private static final String	ABCDEFGHIJKLMNOPQRSTUVWXYZ_ABCDEFGHIJKLMNOPQRSTUVWXYZ	= "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-._$@%+";

	public void testValidName() throws UnsupportedEncodingException {
		assertEquals( "", WebresourceServlet.toValidFileName(""));
		assertEquals( ABCDEFGHIJKLMNOPQRSTUVWXYZ_ABCDEFGHIJKLMNOPQRSTUVWXYZ,WebresourceServlet.toValidFileName(ABCDEFGHIJKLMNOPQRSTUVWXYZ_ABCDEFGHIJKLMNOPQRSTUVWXYZ));
		assertEquals("%2a.js", WebresourceServlet.toValidFileName("*.js"));
		assertEquals("%3f.js", WebresourceServlet.toValidFileName("?.js"));
	}
}
