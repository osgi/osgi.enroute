package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.Logarithmic;
import osgi.enroute.quantity.base.util.Quantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnnamedQuantity;

/**
 * The neper (unit symbol Np) is a logarithmic unit for ratios of measurements
 * of physical field and power quantities, such as gain and loss of electronic
 * signals. The unit's name is derived from the name of John Napier, the
 * inventor of logarithms. As is the case for the decibel and bel, the neper is
 * a unit of the International System of Quantities (ISQ), but not part of the
 * International System of Units (SI), but it is accepted for use alongside the
 * SI.[1]
 *
 */
public class Neper extends Quantity<Neper> {
	public static final AbstractConverter<Neper> DEFAULT_CONVERTER = null; // TODO

	Neper(double value) {
		super(value);
	}

	private static final long serialVersionUID = 1L;

	@Override
	protected Neper same(double value) {
		return Neper.from(value);
	}

	public static Neper from(double value) {
		return new Neper(value);
	}

	@Override
	public Unit getUnit() {
		return Unit.DIMENSIONLESS;
	}

	@Override
	public Neper mul(double value) {
		return Neper.from(this.value + Math.log(value));
	}

	public Neper mul(Neper neper) {
		return Neper.from(this.value + neper.value);
	}

	public Neper mul(Logarithmic<?> src) throws Exception {
		return Neper.from(this.value + src.toNeper());
	}

	public Neper div(Logarithmic<?> src) throws Exception {
		return Neper.from(this.value - src.toNeper());
	}

	@Override
	public Neper div(double value) {
		return Neper.from(this.value - Math.log(value));
	}

	public Neper div(Neper neper) {
		return Neper.from(this.value - neper.value);
	}

	@Override
	@Deprecated
	public Neper add(Neper mul) {
		return super.add(mul);
	}

	@Override
	@Deprecated
	public Neper sub(Neper mul) {
		return super.sub(mul);
	}

	@Deprecated
	public Quantity<?> mul(Quantity<?> src) throws Exception {
		return UnnamedQuantity.from(value * src.value, getUnit().add(src.getUnit()));
	}

	@Deprecated
	public Quantity<?> div(Quantity<?> src) throws Exception {
		return UnnamedQuantity.from(value / src.value, getUnit().add(src.getUnit()));
	}

}
