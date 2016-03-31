package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit = "m³", symbol = "Volume?", dimension = "Volume", symbolForDimension = "A")
public class Volume extends DerivedQuantity<Volume> {

	private static final long				serialVersionUID	= 1L;
	private static final Unit				unit				= new Unit(Volume.class, Length.DIMe3);
	public static final AbstractConverter<Volume>	DEFAULT_CONVERTER	= null;									// TODO

	Volume(double value) {
		super(value);
	}

	public static Volume from(double value) {
		return new Volume(value);
	}

	@Override
	protected Volume same(double value) {
		return from(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public Area div(Length length) {
		return Area.fromMeter2(value / length.value);
	}

	public Length cbrt() {
		return Length.fromMeter(Math.cbrt(value));
	}

	public static Volume fromLitre(double value) {
		return from(value / 1000d);
	}

	public double toLitre() {
		return value * 1000;
	}
}
