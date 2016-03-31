package osgi.enroute.quantity.base.util;

@UnitInfo(name="unity",description="Dimensionless", symbol="", unit="1", symbolForDimension="1", dimension = "" )
public class Unity extends Quantity<Unity> {
	private static final long serialVersionUID = 1L;
	public static final AbstractConverter<Unity> DEFAULT_CONVERTER = null; // TODO
	private static Unit unit = Unit.DIMENSIONLESS;
	public static Unity ZERO = new Unity(0);
	public static Unity ONE = new Unity(1);
	public static Unity TWO = new Unity(2);
	public static Unity THREE = new Unity(3);
	public static Unity FOUR= new Unity(4);
	public static Unity FIVE = new Unity(5);
	public static Unity SIX = new Unity(6);
	public static Unity SEVEN = new Unity(7);
	public static Unity EIGHT = new Unity(8);
	public static Unity NINE = new Unity(9);
	public static Unity TEN = new Unity(10);
	
	Unity(double value) {
		super(value);
	}


	@Override
	protected Unity same(double value) {
		return null;
	}

	@Override
	public Unit getUnit() {
		return unit;
	}
	
	public static Unity from( int n ) {
		switch ( n) {
		case 0: return ZERO;
		case 1: return ONE;
		case 2: return TWO;
		case 3: return THREE;
		case 4: return FOUR;
		case 5: return FIVE;
		case 6: return SIX;
		case 7: return SEVEN;
		case 8: return EIGHT;
		case 9: return NINE;
		case 10: return TEN;
		}
		return from( (double) n);
	}


	private static Unity from(double n) {
		return new Unity(n);
	}
	
	public static Unity fromPercent( double value ) {
		return from(value / 100);
	}

	public  double toPercent() {
		return value * 100;
	}

}
