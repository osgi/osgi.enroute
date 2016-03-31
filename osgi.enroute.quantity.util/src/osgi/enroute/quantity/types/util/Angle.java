package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.BaseQuantity;
import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit="rad", symbol="θ", dimension="Angle", symbolForDimension="1")
public class Angle extends BaseQuantity<Angle>{
	private static final long serialVersionUID = 1L;
	private static final Unit unit = new Unit(Angle.class);
	
	public static AbstractConverter<Angle>	DEFAULT_CONVERTER	= new AbstractConverter<>(Angle.class,
			Angle::fromRadian, Angle::toRadian, "rad");

	Angle(double value) {
		super(value);
	}

	
	@Override
	protected Angle same(double value) {
		return fromRadian(value);
	}

	static public Angle fromRadian(double value) {
		return new Angle(value);
	}

	public double toRadian() {
		return value;
	}

	@Override
	public Unit getUnit() {
		return unit;
	}
	
	public static Angle fromDegree( double degree ) {		
		return Angle.fromRadian( Math.toRadians(degree) );
	}
	
	public double toDegree() {
		return Math.toDegrees(value);
	}

	
	public Angle fromGradian( double degree ) {
		return Angle.fromRadian( degree * 0.015708d);
	}
	
	public double toGradian() {
		return value / 0.015708d;
	}

	
	public Angle fromMinuteOfArc( double minuteOfArc ) {
		return Angle.fromRadian( minuteOfArc * 0.000290888d);
	}
	
	public double toMinuteOfArc() {
		return value / 0.000290888d;
	}

	public Angle fromSecondOfArc( double minuteOfArc ) {
		return Angle.fromRadian( minuteOfArc * 0.0166667d);
	}
	
	public double toSecondOfArc() {
		return value / 0.0166667d;
	}

	
	public Angle fromAnglemil( double anglemil ) {
		return Angle.fromRadian( anglemil * 0.000159155d);
	}
	
	public double toAnglemil() {
		return value / 0.000159155d;
	}


	public double sin() {
		return Math.sin(value);
	}

	public double cos() {
		return Math.cos(value);
	}

	public double tan() {
		return Math.tan(value);
	}
	
}
