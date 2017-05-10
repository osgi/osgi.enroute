package osgi.enroute.iot.lego.adapter;

import junit.framework.TestCase;
import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;

public class LegoRCTest extends TestCase {
	
	private LegoRC legoRC;
	private int[] output;

	public void setUp() {
		legoRC = new LegoRC();
		CircuitBoard cb = new CircuitBoard() {


			@Override
			public boolean fire(IC ic, String pin, Object value) {
				output = (int[]) value;
				return false;
			}
			
		};
		legoRC.setCircuitBoard(cb);
	}
	
	public void testSimple() throws Exception {
		legoRC.A(1.0/7.0);
		
		assertNotNull(output);
		assertEquals(215, output.length);
	}
	
	public void testList() {
		double width = 1000000d / 38000d / 2d;
		
		for ( int i=0; i< 12; i++)
			System.out.printf("%2d %s%n", i, width * i);
	}
}
