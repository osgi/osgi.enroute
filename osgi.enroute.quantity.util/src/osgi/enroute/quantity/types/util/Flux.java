package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit="Wb", symbol="Φ", dimension="Magnetic Flux", symbolForDimension="")
public class Flux extends DerivedQuantity<Flux> {
	private static final long		serialVersionUID	= 1;
	private static final Unit	dimension			= new Unit(Flux.class);
	public static final AbstractConverter<Flux> DEFAULT_CONVERTER = null; // TODO

	Flux(double value) {
		super(value);
	}

	@Override
	protected Flux same(double value) {
		return from(value);
	}

	private Flux from(double value) {
		return new Flux(value);
	}

	@Override
	public Unit getUnit() {
		return dimension;
	}

}
