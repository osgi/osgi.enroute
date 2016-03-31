package osgi.enroute.quantity.base.util;

public class UnnamedQuantity extends DerivedQuantity<UnnamedQuantity> {
	private static final long serialVersionUID = 1L;
	final Unit unit;
	
	public UnnamedQuantity(double value, Unit unit) {
		super(value);
		this.unit=unit;
	}

	@Override
	protected UnnamedQuantity same(double value) {
		return new UnnamedQuantity(value, unit);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public static UnnamedQuantity from(double value, Unit unit) {
		return new UnnamedQuantity(value, unit);
	}

}
