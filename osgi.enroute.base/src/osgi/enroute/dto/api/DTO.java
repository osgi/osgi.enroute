package osgi.enroute.dto.api;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

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
public class DTO {
	private final static WeakHashMap<Class< ? >,Field[]>	cache	= new WeakHashMap<>();
	private final Field[]									primaries;

	protected DTO() {
		Field fields[] = DTO.cache.get(getClass());
		if (fields == null) {
			fields = getClass().getFields();

			int primary = 0;
			for (Field f : fields) {
				Primary p = f.getAnnotation(Primary.class);
				if (p != null)
					primary++;
			}

			Field[] tmpfields = fields;
			if (primary != 0) {
				tmpfields = new Field[primary];
				int i = 0;
				for (Field f : fields) {
					Primary p = f.getAnnotation(Primary.class);
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

			DTO.cache.put(getClass(), tmpfields);
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

	public String toString() {
		try {
			StringBuilder sb = new StringBuilder();
			toString(sb, 0, this);
			return sb.toString();
		}
		catch (Exception e) {
			return e.getMessage();
		}
	}

	private static void toString(StringBuilder sb, int i, Object o) throws IllegalArgumentException,
			IllegalAccessException {
		if (o instanceof Map)
			map(sb, i, (Map< ? , ? >) o);
		else if (o instanceof List< ? >)
			list(sb, i, (List< ? >) o);
		else if (o instanceof DTO)
			dto(sb, i, (DTO) o);
		else {
			String s = o.toString();
			if (s.length() > 200) {
				s = s.substring(0, 100) + "..." + s.substring(s.length() - 100);
			}
			sb.append(s);
		}
	}

	private static void dto(StringBuilder sb, int i, DTO o) throws IllegalArgumentException, IllegalAccessException {
		sb.append("{");
		String del = "\n";
		boolean had = false;
		for (Field f : o.primaries) {
			had = true;
			sb.append(del);
			indent(sb, i + 1);
			sb.append(f.getName()).append(": ");
			toString(sb, i + 1, f.get(o));
			del = ",\n";
		}
		if (!had)
			sb.append("}");
		else {
			sb.append("\n");
			indent(sb, i);
			sb.append("}");
		}
	}

	private static void indent(StringBuilder sb, int i) {
		while (i > 0)
			sb.append("  ");
	}

	private static void list(StringBuilder sb, int i, List< ? > o) throws IllegalArgumentException,
			IllegalAccessException {
		sb.append("[");
		String del = "\n";
		boolean had = false;
		for (Object x : o) {
			had = true;
			sb.append(del);
			indent(sb, i + 1);
			toString(sb, i + 1, x);
			del = ",\n";
		}
		if (!had)
			sb.append("]");
		else {
			sb.append("\n");
			indent(sb, i);
			sb.append("]");
		}
	}

	private static void map(StringBuilder sb, int i, Map< ? , ? > o) throws IllegalArgumentException,
			IllegalAccessException {
		sb.append("{");
		String del = "\n";
		boolean had = false;
		for (Entry< ? , ? > e : o.entrySet()) {
			had = true;
			sb.append(del);
			indent(sb, i + 1);
			sb.append(e.getKey()).append("= ");
			toString(sb, i + 1, e.getValue());
			del = ",\n";
		}
		if (!had)
			sb.append("}");
		else {
			sb.append("\n");
			indent(sb, i);
			sb.append("}");
		}
	}

	public <T> List<T> list() {
		return new ArrayList<>();
	}

	public <K, V> Map<K,V> map() {
		return new LinkedHashMap<>();
	}

	public <T> Set<T> set() {
		return new LinkedHashSet<>();
	}
}
