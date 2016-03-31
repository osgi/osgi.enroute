package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit="T", symbol="B", dimension="Magnetic Field", symbolForDimension="")
public class MagneticField extends DerivedQuantity<MagneticField>{
	private static final long serialVersionUID = 1L;
	private static final Unit unit = new Unit(MagneticField.class, Mass.DIMe1, Current.DIMe_1, Time.DIMe_2);
	public static final AbstractConverter<MagneticField> DEFAULT_CONVERTER = null; // TODO

	public MagneticField(double value) {
		super(value);
	}

	@Override
	protected MagneticField same(double value) {
		return from(value);
	}

	public static MagneticField from(double value) {
		return new MagneticField(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

}
