package osgi.enroute.quantity.base.util;

/**
 * Root for all base units.
 * 
 * @param <T> the actual unit
 */
public abstract class BaseQuantity<T extends BaseQuantity<T>> extends Quantity<T> {
	private static final long serialVersionUID = 1L;

	public BaseQuantity(double value) {
		super(value);
	}
	
}
