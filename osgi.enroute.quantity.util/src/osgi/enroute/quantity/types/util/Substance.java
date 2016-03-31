package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.BaseQuantity;
import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;
import osgi.enroute.quantity.base.util.Unit.Dimension;

@UnitInfo(unit = "mol", symbol="n", dimension = "Amount of substance", symbolForDimension = "N")
public class Substance extends BaseQuantity<Substance> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(Substance.class);
	public static final Dimension		DIMe1				= Unit.dimension(Substance.class, 1);
	public static final AbstractConverter<Substance> DEFAULT_CONVERTER = null; // TODO

	Substance(double value) {
		super(value);
	}

	@Override
	protected Substance same(double value) {
		return from(value);
	}

	public static Substance from(double value) {
		return new Substance(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

}
