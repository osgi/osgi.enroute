package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

/**
 * The lux (symbol: lx) is the SI unit of illuminance and luminous emittance,
 * measuring luminous flux per unit area. It is equal to one lumen per square
 * metre. In photometry, this is used as a measure of the intensity, as
 * perceived by the human eye, of light that hits or passes through a surface.
 * It is analogous to the radiometric unit watts per square metre, but with the
 * power at each wavelength weighted according to the luminosity function, a
 * standardized model of human visual brightness perception. In English, "lux"
 * is used in both singular and plural
 * 
 */
@UnitInfo(unit = "lx", symbol="Ev", dimension = "Luminance", symbolForDimension = "L", description="Total luminous flux incident to a surface per unit area")
public class Luminance extends DerivedQuantity<Luminance> {
	private static final long	serialVersionUID	= 1L;
	private static Unit			unit				= new Unit(Luminance.class, LuminousIntensity.DIMe1, Length.DIMe_2);
	public static final AbstractConverter<Luminance> DEFAULT_CONVERTER = null; // TODO

	Luminance(double value) {
		super(value);
	}

	@Override
	protected Luminance same(double value) {
		return from(value);
	}

	public static Luminance from(double value) {
		return new Luminance(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public double toStibs() {
		return value / 10e-4d;
	}

	public static Luminance fromStibs(double value) {
		return from(value * 10e4);
	}

	public double toApotibs() {
		return Math.PI * value;
	}

	public static Luminance fromApotibs(double value) {
		return from(value / Math.PI);
	}

	public double toLamberts() {
		return Math.PI * value * 10e-4d;
	}

	public static Luminance fromLamberts(double value) {
		return from(value / Math.PI / 10e-4d);
	}

	public double toFootLamberts() {
		return value * 0.292d;
	}

	public static Luminance fromFootLamberts(double value) {
		return from(value / 0.292d);
	}
}
