package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.BaseQuantity;
import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit = "J",symbol="W",  dimension = "Energy", symbolForDimension = "")
public class Energy extends BaseQuantity<Energy> {
	private static final long	serialVersionUID	= 1L;
	private static Unit			unit			= new Unit(Energy.class,	Mass.DIMe1, Length.DIMe1, Time.DIMe_2);
	private static final double	JOULE_TO_KWH		= 2.77778e-7;
	private static final double	JOULE_TO_CALORIE	= 0.239006;

	public final static Energy ZERO = new Energy(0d);
	public final static Energy ONE = new Energy(1d);
	public static final AbstractConverter<Energy> DEFAULT_CONVERTER = null; // TODO

	Energy(double value) {
		super(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public static Energy fromJoule(double value) {
		return new Energy(value);
	}

	public static Energy fromKiloWattHour(double value) {
		return fromJoule(value / JOULE_TO_KWH);
	}

	public double toKiloWattHour() {
		return value * JOULE_TO_KWH;
	}

	public double toCalorie() {
		return value * JOULE_TO_CALORIE;
	}

	public static Energy fromCalorie(double value) {
		return fromJoule(value / JOULE_TO_KWH);
	}

	@Override
	protected Energy same(double value) {
		return fromJoule(value);
	}

	public static Energy fromKilojoule(double v) {
		return Energy.fromJoule(v * 1000d);
	}

	public double toKilojoule() {
		return value / 1000d;
	}

	public static Energy fromKilocalorie(double v) {
		return Energy.fromJoule(v * 4184d);
	}

	public double toKilocalorie() {
		return value / 4184d;
	}

	public static Energy fromWatthour(double v) {
		return Energy.fromJoule(v * 3600);
	}

	public double toWatthour() {
		return value / 3600;
	}

	public static Energy fromElectronVolt(double v) {
		return Energy.fromJoule(v * 1.6022e-19d);
	}

	public double toElectronVolt() {
		return value / 1.6022e-19;
	}

	public static Energy fromBritishThermalUnit(double v) {
		return Energy.fromJoule(v * 1055.071288087d);
	}

	public double toBritishThermalUnit() {
		return value / 1055.071288087d;
	}

	public static Energy fromUSThermalUnit(double v) {
		return Energy.fromJoule(v * 1.055e+8d);
	}

	public double toUSThermalUnit() {
		return value / 1.055e+8d;
	}

	public static Energy fromFootPound(double v) {
		return Energy.fromJoule(v * 1.35582d);
	}

	public double toFootPound() {
		return value / 1.35582d;
	}
	
	public static Energy fromVAh(double value) {
		return fromJoule( value * 3600);
	}
	
	public double toVAh() {
		return value / 3600;
	}
	
	public static Energy fromWh( double value) {
		return fromJoule( value * 3600);
	}
	
	public double toWh() {
		return value / 3600;
	}

	public static Energy fromVarPerHour( double value ) {
		return fromJoule( value * 3600 );
	}
	public double toVarPerHour() {
		return value / 3600;
	}
}
