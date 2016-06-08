package osgi.enroute.quantity.base.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Unit {
	final private Dimension[]														dimensions;
	final private Unit																inverse;
	@SuppressWarnings("unused")
	private static final String														EXPONENT	= "⁰¹²³⁴⁵⁶⁷⁸⁹";

	public static final Unit DIMENSIONLESS = new Unit(Unity.class, new Dimension[0]);

	private String		unit;
	private UnitInfo	info;

	static public class Dimension implements Comparable<Dimension> {
		final int								exponent;
		final Class<? extends BaseQuantity<?>>	unit;

		Dimension(Class<? extends BaseQuantity<?>> unit, int exponent) {
			this.unit = unit;
			this.exponent = exponent;
		}

		@Override
		public int compareTo(Dimension o) {
			if (this == o)
				return 0;

			if (this.unit.equals(o.unit)) {
				return Integer.compare(this.exponent, o.exponent);
			}

			int n = this.unit.getName().compareTo(o.unit.getName());
			if (n == 0)
				return 0;

			return Integer.compare(this.exponent, o.exponent);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + exponent;
			result = prime * result + ((unit == null) ? 0 : unit.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Dimension other = (Dimension) obj;
			if (exponent != other.exponent)
				return false;
			if (unit == null) {
				if (other.unit != null)
					return false;
			} else if (!unit.equals(other.unit))
				return false;
			return true;
		}

		public String toString() {
			UnitInfo ui = unit.getAnnotation(UnitInfo.class);
			if ( ui != null) {
				return ui + "^" + exponent;
			}
			return "?"+ "^" + exponent;
		}
	}

	public Unit(Class<? extends Quantity<?>> type, Dimension... units) {
		this.info = type.getAnnotation(UnitInfo.class);
		if (this.info == null)
			throw new IllegalArgumentException("A QuantityType must be annotated with a UnitInfo: " + type);

		Arrays.sort(units);
		this.dimensions = units;
		this.inverse = new Unit(this, units);
		this.unit = this.info.unit();
	}

	public Unit(Class<? extends BaseQuantity<?>> type) {
		this(type, Unit.dimension(type, 1));
		this.unit = this.info.unit();
	}

	private Unit(Unit normal, Dimension[] units) {
		this.dimensions = new Dimension[units.length];
		for (int i = 0; i < units.length; i++) {
			this.dimensions[i] = new Dimension(units[i].unit, -units[i].exponent);
		}
		this.inverse = normal;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(dimensions);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Unit other = (Unit) obj;
		if (!Arrays.equals(dimensions, other.dimensions))
			return false;
		return true;
	}

	Unit inverse() {
		return inverse;
	}

	// TODO
	public boolean isCompatible(Unit a, Unit b) {
		Unit r = a.add(b);
		return isCompatible(r);
	}

	public boolean isCompatible(Unit r) {
		if (dimensions.length != r.dimensions.length)
			return false;

		for (int i = 0; i < dimensions.length; i++) {
			if (!dimensions[i].equals(r.dimensions[i]))
				return false;
		}
		return true;
	}

	public Unit add(Unit b) {
		Dimension[] dims = merge(dimensions, b.dimensions);
		return new Unit(this, dims);
	}

	public Unit sub(Unit b) {
		return add(b.inverse);
	}

	// size of C array must be equal or greater than
	// sum of A and B arrays' sizes
	private Dimension[] merge(Dimension[] a, Dimension[] b) {
		List<Dimension> list = new ArrayList<>();
		int i, j, m, n;
		i = 0;
		j = 0;
		m = a.length;
		n = b.length;

		while (i < m && j < n) {
			int compareTo = a[i].compareTo(b[j]);
			if (compareTo == 0) {
				list.add(new Dimension(a[i].unit, a[i++].exponent + b[j++].exponent));
			} else if (compareTo > 0) {
				list.add(a[i++]);
			} else {
				list.add(b[j++]);
			}
		}
		if (i < m) {
			for (int p = i; p < m; p++) {
				list.add(a[p]);
			}
		} else {
			for (int p = j; p < n; p++) {
				list.add(b[p]);
			}
		}
		return list.toArray(new Dimension[list.size()]);
	}

	public static Dimension dimension(Class<? extends BaseQuantity<?>> c, int exponent) {
		return new Dimension(c, exponent);
	}

	// not locked so might happen multiple times
	public String toString() {
		return unit;
	}

}
