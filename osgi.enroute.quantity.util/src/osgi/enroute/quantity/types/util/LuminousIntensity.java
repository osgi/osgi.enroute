package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.BaseQuantity;
import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;
import osgi.enroute.quantity.base.util.Unit.Dimension;

/**
 * The candela (/kænˈdɛlə/ or /kænˈdiːlə/; symbol: cd) is the SI base unit of
 * luminous intensity; that is, luminous power per unit solid angle emitted by a
 * point light source in a particular direction.
 * 
 */
@UnitInfo(unit="cd", symbol="L", dimension = "Luminous Intensity", symbolForDimension = "J")
public class LuminousIntensity extends BaseQuantity<LuminousIntensity> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(LuminousIntensity.class);
	public static final Dimension		DIMe1				= Unit.dimension(LuminousIntensity.class, 1);
	public static final AbstractConverter<LuminousIntensity> DEFAULT_CONVERTER = null; // TODO

	LuminousIntensity(double value) {
		super(value);
	}

	LuminousIntensity() {
		super(0D);
	}

	@Override
	protected LuminousIntensity same(double value) {
		return LuminousIntensity.from(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public static LuminousIntensity from(double value) {
		return new LuminousIntensity(value);
	}
}
