package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

/**
 * The SI system defines the coulomb in terms of the ampere and second: 1 C = 1
 * A × 1 s.
 * 
 * Since the charge of one electron is known to be about −1.6021766208(98)×10−19
 * C,[7] −1 C can also be considered the charge of roughly 6.241509×1018
 * electrons (or +1 C the charge of that many positrons or protons), where the
 * number is the reciprocal of 1.602177×10−19.
 * 
 */
@UnitInfo(unit = "C", symbol = "Q", dimension = "Electric charge", symbolForDimension = "?")
public class Charge extends DerivedQuantity<Charge> {

	public static Charge ELEMENTARY_CHARGE = new Charge(1.602176620898E-19D);
	public static final AbstractConverter<Charge> DEFAULT_CONVERTER = null; // TODO

	private static final long	serialVersionUID	= 1L;
	final static Unit			unit				= new Unit(Charge.class,	//
			//
			Current.DIMe1,														//
			Time.DIMe1

														);

	Charge(double value) {
		super(value);
	}

	public static Charge fromCoulomb(double value) {
		return new Charge(value);
	}

	@Override
	protected Charge same(double value) {
		return fromCoulomb(value);
	}
	
	public double toCoulomb() {
		return value;
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public static Charge fromAmpereHour(double value) {
		return new Charge(value * 3600);
	}

	public double toAmpereHour() {
		return this.value / 3600;
	}
	
	public static Charge fromKiloAmpereHour( double value ) {
		return fromCoulomb( value * 1000 * 3600);
	}
	
	public double toKiloAmpereHour() {
		return value / 1000 / 3600;
	}
	
	public static Charge fromAh(double value) {
		return new Charge(value * 3600);
	}

	public double toAh() {
		return value/3600;
	}
}
