package osgi.enroute.web.server.provider;

import java.io.*;

import junit.framework.*;

public class SimpleImplTest extends TestCase {

	private static final String	ABCDEFGHIJKLMNOPQRSTUVWXYZ_ABCDEFGHIJKLMNOPQRSTUVWXYZ	= "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-._$@%+";

	public void testValidName() throws UnsupportedEncodingException {
		assertEquals( "", WebresourceServer.toValidFileName(""));
		assertEquals( ABCDEFGHIJKLMNOPQRSTUVWXYZ_ABCDEFGHIJKLMNOPQRSTUVWXYZ,WebresourceServer.toValidFileName(ABCDEFGHIJKLMNOPQRSTUVWXYZ_ABCDEFGHIJKLMNOPQRSTUVWXYZ));
		assertEquals("%2a.js", WebresourceServer.toValidFileName("*.js"));
		assertEquals("%3f.js", WebresourceServer.toValidFileName("?.js"));
	}
}
