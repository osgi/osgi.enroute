package osgi.enroute.quantity.base.util;

public abstract class DerivedQuantity<T extends DerivedQuantity<T>> extends Quantity<T> {
	private static final long serialVersionUID = 1L;

	public DerivedQuantity(double value) {
		super(value);
	}

}
