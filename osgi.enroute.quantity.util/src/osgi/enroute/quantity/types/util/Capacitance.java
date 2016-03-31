package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

/**
 * One farad is defined as the capacitance of a capacitor across which, when
 * charged with one coulomb of electricity, there is a potential difference of
 * one volt. Conversely, it is the capacitance which, when charged to a
 * potential difference of one volt, carries a charge of one coulomb. A coulomb
 * is equal to the amount of charge (electrons) produced by a current of one
 * ampere (A) flowing for one second. For example, the voltage across the two
 * terminals of a 1 F capacitor will increase linearly by 1 V when a current of
 * 1 A flows through it for 1 second.
 */

@UnitInfo(unit = "F", symbol="C", dimension = "Capacitance", symbolForDimension = "")
public class Capacitance extends DerivedQuantity<Capacitance> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(Capacitance.class,		//
			Time.DIMe4,															//
			Current.DIMe2,															//
			Length.DIMe_2,															//
			Mass.DIMe_1);
	public static final AbstractConverter<Capacitance> DEFAULT_CONVERTER = null; // TODO

	public Capacitance(double value) {
		super(value);
	}

	@Override
	protected Capacitance same(double value) {
		return Capacitance.from(value);
	}

	public static Capacitance from(double value) {
		return new Capacitance(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

}
