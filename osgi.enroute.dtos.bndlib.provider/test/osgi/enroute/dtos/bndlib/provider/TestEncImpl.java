package osgi.enroute.dtos.bndlib.provider;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.osgi.framework.dto.BundleDTO;

import junit.framework.TestCase;
import osgi.enroute.dto.api.DTOs;

/**
 * Basic {@link Enc} test case.
 */
public class TestEncImpl extends TestCase {

	private static final String EXPECTED_ENCODED_CONTENT = "{\"id\":999,\"symbolicName\":\"com.example.test\"}";
	private static final BundleDTO bundleDTO = new BundleDTO();
	static {
		bundleDTO.id = 999;
		bundleDTO.symbolicName = "com.example.test";
	}

	private DTOs.Enc cut;

	public void setUp() throws Exception {
		cut = new DTOsProvider().encoder(bundleDTO);
	}

	public void testAppendable() throws Exception {
		try (Writer writer = new StringWriter()) {
			cut.put(writer);
			assertEquals(EXPECTED_ENCODED_CONTENT, writer.toString());
		}
	}

	public void testOutputStream() throws Exception {
		try (OutputStream os = new ByteArrayOutputStream()) {
			cut.put(os);
			assertEquals(EXPECTED_ENCODED_CONTENT, os.toString());
		}
		try (OutputStream os = new ByteArrayOutputStream()) {
			cut.put(os, "UTF-8");
			assertEquals(EXPECTED_ENCODED_CONTENT, os.toString());
		}
	}

}
