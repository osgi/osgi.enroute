package osgi.enroute.base.dtos.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.dto.DTO;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import aQute.bnd.annotation.component.Reference;
import aQute.bnd.testing.DSTestWiring;
import junit.framework.TestCase;
import osgi.enroute.base.configurer.test.ConfigurerTest;
import osgi.enroute.dto.api.DTOs;
import osgi.enroute.dto.api.DTOs.Difference;
import osgi.enroute.dto.api.TypeReference;

public class DTOsTest extends TestCase {
	BundleContext context = FrameworkUtil.getBundle(ConfigurerTest.class)
			.getBundleContext();
	DSTestWiring ds = new DSTestWiring();
	private DTOs dtos;

	public void setUp() throws Exception {
		ds.setContext(context);
		ds.add(this);
		ds.wire();
	}

	/*
	 * Simple conversion
	 */

	public void testSimple() throws Exception {
		
		assertEquals( 100D, dtos.convert("100").to(double.class));
		assertEquals( 10D, dtos.convert(10f).to(double.class));
		assertEquals( 100D, dtos.convert(100L).to(double.class));
		
		assertEquals( Arrays.asList(100F), 
					dtos.convert(100L).to(new TypeReference<List<Float>>(){}));
	
		
		long[] expected = new long[]{0x40L,  0x41L, 0x42L};
		byte[] source = "@AB".getBytes();
		long[] result = dtos.convert(source).to(long[].class);
		
		assertTrue( Arrays.equals(expected,result ));
	}

	/*
	 * Show Map -> Interface
	 */
	enum Option {
		bar, don, zun
	};

	interface FooMap {
		short port();

		String host();

		Set<Option> options();
	}

	public void testInterfaceAsMap() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put("port", 10);
		map.put("host", "localhost");
		map.put("options", Arrays.asList("bar", "don", "zun"));

		FooMap foomap = dtos.convert(map).to(FooMap.class);

		assertEquals((short) 10, foomap.port());
		assertEquals("localhost", foomap.host());
		assertEquals(EnumSet.allOf(Option.class), foomap.options());
	}

	/*
	 * Show DTO to map
	 */

	public static class MyData extends DTO {
		public short port;
		public String host;
		public Option[] options;
	}

	public void testDtoAsMap() throws Exception {
		MyData m = new MyData();
		m.port = 20;
		m.host = "example.com";
		m.options = new Option[] { Option.bar, Option.don, Option.zun };

		Map<String, Object> map = dtos.asMap(m);

		assertEquals(Arrays.asList("host", "options", "port"),
				new ArrayList<String>(map.keySet()));
		assertEquals((short) 20, map.get("port"));
		assertEquals("example.com", map.get("host"));
	}

	
	/*
	 * Show JSON
	 */
	
	public void testJSON() throws Exception {
		MyData m = new MyData();
		m.port = 20;
		m.host = "example.com";
		m.options = new Option[] { Option.bar, Option.don, Option.zun };
		
		String json = dtos.encoder(m).put();
		assertEquals("{\"host\":\"example.com\",\"options\":[\"bar\",\"don\",\"zun\"],\"port\":20}",json);		
	}
	
	public void testDiff() throws Exception {
		MyData source = new MyData();
		source.port = 20;
		source.host = "example.com";
		source.options = new Option[] { Option.bar, Option.don, Option.zun };

		MyData copy = dtos.deepCopy(source);
		
		assertFalse( source == copy);
		assertTrue( dtos.equals(source,copy));
		
		List<Difference> diff = dtos.diff(source, copy);
		assertEquals(0, diff.size());
		
		copy.port = 10;
		diff = dtos.diff(source, copy);
		assertEquals(1, diff.size());
		assertEquals("port", diff.get(0).path[0]);
	}

	@Reference
	void setDtos(DTOs dtos) {
		this.dtos = dtos;
	}
}
