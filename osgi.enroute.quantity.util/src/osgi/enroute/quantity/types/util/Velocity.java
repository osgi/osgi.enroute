package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit="m/s", symbol="v", dimension="Velocity", symbolForDimension="v")
public class Velocity extends DerivedQuantity<Velocity> {
	private static final long		serialVersionUID	= 1L;
	private static final Unit	unit			= new Unit(Velocity.class, Length.DIMe1, Time.DIMe_1);
	public static final AbstractConverter<Velocity> DEFAULT_CONVERTER = null; // TODO

	Velocity(double value) {
		super(value);
	}

	@Override
	protected Velocity same(double value) {
		return fromMeterPerSecond(value);
	}

	public static Velocity fromMeterPerSecond(double value) {
		return new Velocity(value);
	}

	public double toMeterPerSecond() {
		return value;
	}
	
	@Override
	public Unit getUnit() {
		return unit;
	}

	public static Velocity fromKilometerPerHour( double kmh) {
		return Velocity.fromMeterPerSecond( kmh * 0.277778d); 
	}

	public double toKilometerPerHour() {
		return value / 0.277778d;
	}

	public static Velocity fromMilePerHour( double mileh) {
		return Velocity.fromMeterPerSecond( mileh * 0.447040357632d); 
	}

	public double toMilePerHour() {
		return value / 0.447040357632d;
	}

	public static Velocity fromFootPerSecond( double foots) {
		return Velocity.fromMeterPerSecond(foots * 0.3048d);
	}
	
	public double toFootPerSecond() {
		return value / 0.3048d;
	}
	
	public static Velocity fromKnot( double knot) {
		return Velocity.fromMeterPerSecond(knot * 0.514444d);
	}
	
	public double toKnot() {
		return value / 0.514444d;
	}

}
