package osgi.enroute.base.provider;

import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.osgi.dto.DTO;

import osgi.enroute.base.dto.provider.DTOsProvider;



/*
 * 
 * 
 * 
 */

public class BaseImplTest extends TestCase {

	public static class Simple extends DTO {
		public String string;
		public int integer;
	}
	
	public void testSimple() throws Exception {
		DTOsProvider dtop = new DTOsProvider();
		
		Simple s = new Simple();
		s.string = "abc";
		s.integer = 42;
		Map<String,Object> map = dtop.asMap(s); 
		
		assertNotNull(map);
		assertEquals( 2, map.size());
		assertFalse( map.isEmpty());
		assertEquals( 42, map.get("integer"));
		assertEquals( "abc", map.get("string"));
		
		int n=0;
		for ( Entry<String, Object> e : map.entrySet()) {
			n++;
			switch( e.getKey()) {
			case "integer": 
				assertEquals( 42, e.getValue());
				break;
				
			case "string": 
				assertEquals( "abc", e.getValue());
				break;

			default:
				fail("Unknown key " + e.getKey());
				
			}
			System.out.println(e.getKey());
		}
		assertEquals(2,n);
		
		assertEquals( "{integer=42, string=abc}", map.toString());
	}
}
