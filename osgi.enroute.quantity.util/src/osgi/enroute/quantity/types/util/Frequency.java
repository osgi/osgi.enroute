package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit="Hz", symbol="f", dimension="Frequency", symbolForDimension="")
public class Frequency extends DerivedQuantity<Frequency> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	dimension			= new Unit(Frequency.class, Time.DIMe_1);
	public static final AbstractConverter<Frequency> DEFAULT_CONVERTER = null; // TODO

	public Frequency(double value) {
		super(value);
	}

	@Override
	protected Frequency same(double value) {
		return fromHz(value);
	}
	
	public double toHz() {
		return value;
	}

	public static Frequency fromHz(double value) {
		return new Frequency(value);
	}

	@Override
	public Unit getUnit() {
		return dimension;
	}

	public static Frequency fromKilohertz(double v) {
		return fromHz(v * 1000);
	}

	public double toKilohertz() {
		return value / 1000;
	}

	public static Frequency fromMegahertz(double v) {
		return fromHz(v * 1_000_000);
	}

	public double toMegahertz() {
		return value / 1_000_000;
	}

	public static Frequency fromGigahertz(double v) {
		return fromHz(v * 1_000_000_000);
	}

	public double toGigahertz() {
		return value / 1_000_000_000;
	}

	public Length wavelength(Velocity v) {
		return Length.fromMeter(v.value / value);
	}

	public Length 𝛌(Velocity v) {
		return Length.fromMeter(v.value / value);
	}

	public Velocity velocity(Length wavelength) {
		return Velocity.fromMeterPerSecond(wavelength.value * value);
	}

	public Time inverse() {
		return Time.fromSecond( 1 / value );
	}
}
