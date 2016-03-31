package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit = "H", symbol="L", dimension = "Inductance", symbolForDimension = "L")
public class Inductance extends DerivedQuantity<Inductance> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(Inductance.class, Mass.DIMe1, Length.DIMe2, Time.DIMe_2,
			Current.DIMe_2);
	public static final AbstractConverter<Inductance> DEFAULT_CONVERTER = null; // TODO

	Inductance(double value) {
		super(value);
	}

	@Override
	protected Inductance same(double value) {
		return Inductance.from(value);
	}

	private static Inductance from(double value) {
		return new Inductance(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	
}
