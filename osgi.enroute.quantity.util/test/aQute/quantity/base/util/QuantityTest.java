package aQute.quantity.base.util;

import junit.framework.TestCase;
import osgi.enroute.quantity.types.util.Current;
import osgi.enroute.quantity.types.util.Length;
import osgi.enroute.quantity.types.util.Time;
import osgi.enroute.quantity.types.util.Velocity;

public class QuantityTest extends TestCase {

	public void testBase() {
		Length m = Length.fromMeter(100).kilo();
		System.out.println(m);
		Velocity speedOfLight = Length.fromMeter(299792458).div(Time.SECOND);
		System.out.println(speedOfLight);
		
		Current c = Current.fromAmpere(10);
		System.out.println(c);
	}

}