package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit="Byte/s", symbol="bandwidth", dimension = "Bandwidth", symbolForDimension = "")
public class Bandwidth extends DerivedQuantity<Bandwidth> {
	private static final long serialVersionUID = 1L;
	private static final Unit unit = new Unit(Bandwidth.class, Bytes.DIMe1, Time.DIMe_1);
	public static final AbstractConverter<Bandwidth> DEFAULT_CONVERTER = null; // TODO

	Bandwidth(double value) {
		super(value);
	}

	@Override
	protected Bandwidth same(double value) {
		return Bandwidth.fromBytesPerSecond(value);
	}

	public static Bandwidth fromBytesPerSecond(double value) {
		return new Bandwidth(value);
	}
	
	public double toBytesPerSecond() {
		return value;
	}

	public static Bandwidth fromBitsPerSecond(double value) {
		return new Bandwidth(value);
	}
	
	public double toBitsPerSecond() {
		return (int) (value * 8);
	}

	public static Bandwidth fromMegaBitPerSecond(double value) {
		return new Bandwidth( value * Bytes.MEGA / 8);
	}
	
	public double toMegaBitPerSecond() {
		return value / Bytes.MEGA * 8;
	}
	@Override
	public Unit getUnit() {
		return unit;
	}
	
}
