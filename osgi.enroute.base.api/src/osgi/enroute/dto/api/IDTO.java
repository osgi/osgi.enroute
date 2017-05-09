package osgi.enroute.dto.api;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.osgi.dto.DTO;

/**
 * An optional base class for DTOs.
 * <p>
 * Though any object with public fields (of limited types) can be a DTO, there
 * is sometimes an advantage in having a base class that provides
 * {@link #equals(Object)}, {@link #hashCode()}, and {@link #toString()}. This
 * class can be used by DTOs.
 * <p>
 * TODO TESTING!!! not yet verified
 */
public class IDTO extends DTO {
	private final static WeakHashMap<Class< ? >,Field[]>	cache	= new WeakHashMap<>();
	private final Field[]									primaries;

	protected IDTO() {
		Field fields[] = IDTO.cache.get(getClass());
		if (fields == null) {
			fields = getClass().getFields();

			int primary = 0;
			for (Field f : fields) {
				PrimaryKey p = f.getAnnotation(PrimaryKey.class);
				if (p != null)
					primary++;
			}

			Field[] tmpfields = fields;
			if (primary != 0) {
				tmpfields = new Field[primary];
				int i = 0;
				for (Field f : fields) {
					PrimaryKey p = f.getAnnotation(PrimaryKey.class);
					if (p != null)
						tmpfields[i++] = f;
				}
			}
			Arrays.sort(tmpfields, new Comparator<Field>() {

				@Override
				public int compare(Field o1, Field o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});

			IDTO.cache.put(getClass(), tmpfields);
			fields = tmpfields;
		}
		this.primaries = fields;
	}

	public boolean equals(Object other) {
		if (other == null)
			return false;

		if (other == this)
			return true;

		Class< ? > oc = other.getClass();
		if (oc != getClass())
			return false;

		try {
			for (Field f : primaries) {
				Object a = f.get(this);
				Object b = f.get(other);
				if (a == b)
					continue;

				if (a == null || b == null || !a.equals(b))
					return false;
			}
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	public int hashCode() {
		int prime = 31;
		int result = 1;
		try {
			for (Field f : primaries) {
				Object a = f.get(this);
				result = prime * result + (a == null ? 0 : a.hashCode());
			}
			return result;
		}
		catch (Exception e) {
			return result;
		}
	}

	/**
	 * A helper method to initialize a IDTO field with a list
	 * 
	 * @return a list
	 */
	public static <T> List<T> list() {
		return new ArrayList<>();
	}

	/**
	 * A helper method to initialize a IDTO field with a set
	 * 
	 * @return a list
	 */
	public static <T> Set<T> set() {
		return new LinkedHashSet<>();
	}

	/**
	 * A helper method to initialize a IDTO field with a map
	 * 
	 * @return a list
	 */
	public static <K, V> Map<K,V> map() {
		return new LinkedHashMap<>();
	}
}
