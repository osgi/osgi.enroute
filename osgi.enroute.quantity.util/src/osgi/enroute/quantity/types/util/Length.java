package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.BaseQuantity;
import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;
import osgi.enroute.quantity.base.util.Unit.Dimension;

@UnitInfo(unit = "m", symbol = "l", dimension = "Length", symbolForDimension = "L", description = "The one-dimensional extent of an object")
public class Length extends BaseQuantity<Length> {

	static final Dimension			DIMe1	= Unit.dimension(Length.class, 1);
	static final Dimension			DIMe2	= Unit.dimension(Length.class, 2);
	public static final Dimension	DIMe3	= Unit.dimension(Length.class, 3);;
	static final Dimension			DIMe_1	= Unit.dimension(Length.class, -1);
	public static Dimension			DIMe_2	= Unit.dimension(Length.class, -2);

	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(Length.class);
	public static final AbstractConverter<Length> DEFAULT_CONVERTER = null; // TODO

	Length(double value) {
		super(value);
	}

	public static Length fromMeter(double value) {
		return new Length(value);
	}

	public double toMeter() {
		return value;
	}

	/**
	 * 
	 */

	public Area square() {
		return Area.fromMeter2(value * value);
	}

	public Volume cubic() {
		return Volume.from(value * value * value);
	}

	public Volume volume(Area m2) {
		return Volume.from(value * m2.value);
	}

	@Override
	protected Length same(double value) {
		return fromMeter(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public static Length fromFoot(double feet) {
		return fromMeter(feet * 0.3048D);
	}

	public double toFoot() {
		return value / 0.3048D;
	}

	public static Length fromMile(double mile) {
		return fromMeter(mile * 1609.34D);
	}

	public double toMile() {
		return value / 1609.34D;
	}

	public static Length fromKilometer(double km) {
		return fromMeter(km * 1000D);
	}

	public double toKilometer() {
		return value / 1000D;
	}

	public static Length fromCentimeter(double cm) {
		return Length.fromMeter(cm / 100);
	}

	public double toCentimeter() {
		return value * 100;
	}

	public static Length fromMilliMeter(double mm) {
		return Length.fromMeter(mm / 1000);
	}

	public double toMilliMeter() {
		return value * 1000;
	}

	public static Length fromMicrometer(double µm) {
		return Length.fromMeter(µm / 1_000_000D);
	}

	public double toMicrometer() {
		return value * 1_000_000D;
	}

	public static Length fromNanometer(double nm) {
		return Length.fromMeter(nm / 1_000_000_000D);
	}

	public double toNanometer() {
		return value * 1_000_000_000D;
	}

	public static Length fromPicometer(double pm) {
		return Length.fromMeter(pm / 1_000_000_000_000D);
	}

	public double toPicometer() {
		return value * 1_000_000_000_000D;
	}

	public static Length fromYard(double yard) {
		return Length.fromMeter(yard * 0.9144);
	}

	public double toYard() {
		return value / 0.9144D;
	}

	public static Length fromInch(double inch) {
		return Length.fromMeter(inch * 0.0254D);
	}

	public double toInch() {
		return value / 0.0254D;
	}

	public Length fromNauticalMile(double nm) {
		return Length.fromMeter(nm * 1852D);
	}

	public double toNauticalMile() {
		return value / 1852D;
	}

	public Velocity div(Time second) {
		return Velocity.fromMeterPerSecond(value / second.value);
	}
}
