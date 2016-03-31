package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.BaseQuantity;
import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;
import osgi.enroute.quantity.base.util.Unit.Dimension;

/**
 * Ampere. Unit information Unit system SI base unit Unit of Electric current
 * Symbol A. It is named after André-Marie Ampère (1775–1836), French
 * mathematician and physicist, considered the father of electrodynamics.
 * <p>
 * The ampere is equivalent to one {@link Charge} (roughly 6.241×1018 times the
 * elementary charge) per second. Amperes are used to express flow rate of
 * electric charge. For any point experiencing a current, if the number of
 * charged particles passing through it — or the charge on the particles passing
 * through it — is increased, the amperes of current at that point will
 * proportionately increase.
 * 
 */

@UnitInfo(unit = "A", symbol = "I", dimension = "Current", symbolForDimension = "I")
public class Current extends BaseQuantity<Current> {

	private static final long		serialVersionUID	= 1L;
	private static Unit				unit				= new Unit(Current.class);
	final public static Current		ZERO				= new Current(0D);
	final public static Current		ONE					= new Current(0D);
	static final Dimension			DIMe1				= Unit.dimension(Current.class, 1);
	static final Dimension			DIMe_1				= Unit.dimension(Current.class, -1);
	public static final Dimension	DIMe2				= Unit.dimension(Current.class, 2);
	public static final Dimension	DIMe_2				= Unit.dimension(Current.class, -2);
	public static final AbstractConverter<Current> DEFAULT_CONVERTER = null; // TODO

	Current(double value) {
		super(value);
	}

	Current() {
		super(0D);
	}

	public static Current fromAmpere(double value) {
		if (value == 0D)
			return ZERO;
		if (value == 1D)
			return ONE;

		return new Current(value);
	}
	
	public double toAmpere() {
		return value;
	}

	@Override
	protected Current same(double value) {
		return fromAmpere(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public Charge charge(Time s) {
		return Charge.fromCoulomb(value * s.value);
	}

	public Power power(Potential volt) {
		return Power.fromWatt(value * volt.value);
	}

	// A = C/s => s= C/A
	public Time time(Charge charge) {
		return Time.fromSecond(charge.value / value);
	}

}
