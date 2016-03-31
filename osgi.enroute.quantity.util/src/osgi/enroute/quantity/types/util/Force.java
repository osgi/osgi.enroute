package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit="N", symbol="F", dimension="Force", symbolForDimension="")
public class Force extends DerivedQuantity<Force>{

	private static final long serialVersionUID = 1L;

	private static Unit unit = new Unit(Force.class, Mass.DIMe1, Length.DIMe1, Time.DIMe_2);
	public static final AbstractConverter<Force> DEFAULT_CONVERTER = null; // TODO

	public Force(double value) {
		super(value);
	}

	@Override
	protected Force same(double value) {
		return from(value);
	}

	public static Force from(double value) {
		return new Force(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}
}
