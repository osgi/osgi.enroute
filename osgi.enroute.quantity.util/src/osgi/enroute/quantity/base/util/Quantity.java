package osgi.enroute.quantity.base.util;

public abstract class Quantity<T extends Quantity<T>> extends Number implements Comparable<T>, SIPrefix<T> {
	private static final long serialVersionUID = 1L;

	public final double value;

	public Quantity(double value) {
		this.value = value;
	}

	@Override
	public int intValue() {
		return (int) value;
	}

	@Override
	public long longValue() {
		return (long) value;
	}

	@Override
	public float floatValue() {
		return (float) value;
	}

	@Override
	public double doubleValue() {
		return value;
	}

	public T mul(double mul) {
		return same(value * mul);
	}

	public T mul(long mul) {
		return same(value * mul);
	}

	public T mul(int mul) {
		return same(value * mul);
	}

	public T div(double mul) {
		return same(value / mul);
	}

	public T div(long mul) {
		return same(value / mul);
	}

	public T div(int mul) {
		return same(value / mul);
	}

	public T add(T mul) {
		return same(value + mul.value);
	}

	public T sub(T mul) {
		return same(value - mul.value);
	}

	public <R extends Quantity<R>> R mul(Class<R> c, Quantity<?> q) throws Exception {
		double value = this.value * q.value;
		return to(c, value, getUnit(), q.getUnit());
	}

	public Quantity<?> mul(Quantity<?> src) throws Exception {
		return UnnamedQuantity.from(value * src.value, getUnit().add(src.getUnit()));
	}

	public Quantity<?> div(Quantity<?> src) throws Exception {
		return UnnamedQuantity.from(value / src.value, getUnit().sub(src.getUnit()));
	}

	public <R extends Quantity<?>> R to(Class<R> c, double value, Unit a, Unit b) throws Exception {
		R result = c.getConstructor(double.class).newInstance(value);

		if (result.getUnit().isCompatible(a, b))
			return result;

		throw new IllegalArgumentException("Target quantity of different dimension than expected. Expected: " + a.add(b)
				+ ", got: " + result.getUnit());
	}

	@Override
	public int hashCode() {
		return Double.hashCode(value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Quantity<?> other = (Quantity<?>) obj;
		if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value))
			return false;
		return true;
	}

	public int compareTo(T t) {
		return Double.compare(value, t.value);
	}

	protected abstract T same(double value);

	public abstract Unit getUnit();

	public <I extends Quantity<I>> I inverse(Class<I> c) throws Exception {
		return to(c, 1 / value, Unit.DIMENSIONLESS, getUnit().inverse());
	}

	public Quantity<?> inverse() throws Exception {
		return UnnamedQuantity.from(1 / value, getUnit().inverse());
	}

	public <I extends Quantity<I>> I square(Class<I> c) throws Exception {
		return to(c, value * value, getUnit(), getUnit());
	}

	public Quantity<?> square() throws Exception {
		return UnnamedQuantity.from(value * value, getUnit().add(getUnit()));
	}

	public <I extends Quantity<I>> I sqrt(Class<I> c) throws Exception {
		return to(c, Math.sqrt(value), getUnit(), getUnit().inverse());
	}

	public Quantity<?> sqrt() throws Exception {
		return UnnamedQuantity.from(Math.sqrt(value), getUnit().sub(getUnit()));
	}

	public <I extends Quantity<I>> I cube(Class<I> c) throws Exception {
		return to(c, value * value, getUnit(), getUnit());
	}

	public Quantity<?> cube() throws Exception {
		return UnnamedQuantity.from(value * value * value, getUnit().add(getUnit()).add(getUnit()));
	}

	public double value() {
		return value;
	}

	// 10 = 1E1
	public Modifier scale() {
		int log10 = (int) Math.log10(value);
		switch (log10) {
		case -24:
		case -23:
		case -22:
			return SIPrefix.Modifier.YOCTO;
		case -21:
		case -20:
		case -19:
			return SIPrefix.Modifier.ZEPTO;
		case -18:
		case -17:
		case -16:
			return SIPrefix.Modifier.ATTO;
		case -15:
		case -14:
		case -13:
			return SIPrefix.Modifier.FEMTO;
		case -12:
		case -11:
		case -10:
			return SIPrefix.Modifier.PICO;
		case -9:
		case -8:
		case -7:
			return SIPrefix.Modifier.NANO;

		case -6:
		case -5:
		case -4:
			return SIPrefix.Modifier.MICRO;

		case -3:
		case -2:
			return SIPrefix.Modifier.MILLI;
		case -1:
			return SIPrefix.Modifier.CENTI;

		case 0:
		case 1:
		case 2:
			return SIPrefix.Modifier.UNIT;

		case 3:
		case 4:
		case 5:
			return SIPrefix.Modifier.KILO;

		case 6:
		case 7:
		case 8:
			return SIPrefix.Modifier.MEGA;

		case 9:
		case 10:
		case 11:
			return SIPrefix.Modifier.GIGA;

		case 12:
		case 13:
		case 14:
			return SIPrefix.Modifier.TERA;

		case 15:
		case 16:
		case 17:
			return SIPrefix.Modifier.PETA;

		case 18:
		case 19:
		case 20:
			return SIPrefix.Modifier.EXA;

		case 21:
		case 22:
		case 23:
			return SIPrefix.Modifier.ZETTA;

		case 24:
		case 25:
		case 26:
			return SIPrefix.Modifier.YOTTA;

		default:
			return SIPrefix.Modifier.UNIT;
		}
	}
	
	public String toString() {
		Modifier m = scale();
		double v= value / m.multiplier;
		return String.format("%.2f %s%s", v, m.symbol, getUnit().toString());
	}
}
