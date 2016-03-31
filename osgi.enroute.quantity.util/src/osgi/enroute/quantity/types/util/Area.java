package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit = "m²", symbol = "Area", dimension = "Area", symbolForDimension = "A")
public class Area extends DerivedQuantity<Area> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(Length.class, Length.DIMe2);
	public static final AbstractConverter<Area> DEFAULT_CONVERTER = null; // TODO

	Area(double value) {
		super(value);
	}

	@Override
	protected Area same(double value) {
		return fromMeter2(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public static Area fromMeter2(double value) {
		return new Area(value);
	}

	public double toMeter2() {
		return value;
	}

	public Volume volume(Length m) {
		return Volume.from(value * m.value);
	}

	public Length sqrt() {
		return Length.fromMeter(Math.sqrt(value));
	}

	public static Area fromKilometer2(double km2) {
		return Area.fromMeter2(km2 * 1_000_000d);
	}

	public double toKilometer2() {
		return value / 1_000_000d;
	}

	public static Area fromDecimeter2(double dm2) {
		return Area.fromMeter2(dm2 / (10 * 10));
	}

	public double toDecimeter2() {
		return value * (10 * 10);
	}

	public static Area fromCentimeter2(double cm2) {
		return Area.fromMeter2(cm2 / (100 * 100));
	}

	public double toCentimeter2() {
		return value * (100 * 100);
	}

	public static Area fromMillimeter2(double mm2) {
		return Area.fromMeter2(mm2 / (1000 * 1000));
	}

	public double toMillimeter2() {
		return value * (1000 * 1000);
	}

	public static Area fromMile2(double mile2) {
		return Area.fromMeter2(mile2 * 2.59e+6d);
	}

	public double toMile2() {
		return value / 2.59e+6d;
	}

	public static Area fromYard2(double yard2) {
		return Area.fromMeter2(yard2 * 0.836127d);
	}

	public double toYard2() {
		return value / 0.836127d;
	}

	public static Area fromFoot2(double v) {
		return Area.fromMeter2(v * 0.092903d);
	}

	public double toFoot2() {
		return value / 0.092903d;
	}

	public static Area fromInch2(double v) {
		return Area.fromMeter2(v * 0.00064516d);
	}

	public double toInch2() {
		return value / 0.00064516d;
	}

	public static Area fromHectare(double v) {
		return Area.fromMeter2(v * 10000d);
	}

	public double toHectare() {
		return value / 10000d;
	}

	public static Area fromAcre(double v) {
		return Area.fromMeter2(v * 4046.86d);
	}

	public double toAcre() {
		return value / 4046.86d;
	}
}
