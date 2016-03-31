package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit = "m/s²", symbol = "a", dimension = "Acceleration", symbolForDimension = "")
public class Acceleration extends DerivedQuantity<Acceleration> {
	private static final long				serialVersionUID	= 1L;
	private static final Unit				unit				= new Unit(Acceleration.class, Length.DIMe1,
			Time.DIMe_2);
	public static AbstractConverter<Acceleration>	DEFAULT_CONVERTER	= new AbstractConverter<>(Acceleration.class,
			Acceleration::fromMeterPerSecond2, Acceleration::toMeterPerSecond2, "m/s²");

	public Acceleration(double value) {
		super(value);
	}

	@Override
	protected Acceleration same(double value) {
		return fromMeterPerSecond2(value);
	}

	public static Acceleration fromMeterPerSecond2(double value) {
		return new Acceleration(value);
	}

	public double toMeterPerSecond2() {
		return value;
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

}
