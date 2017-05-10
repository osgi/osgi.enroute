package osgi.enroute.dtos.bndlib.provider;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import org.osgi.dto.DTO;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.lib.json.Decoder;
import aQute.lib.json.Encoder;
import aQute.lib.json.JSONCodec;
import osgi.enroute.dto.api.DTOs;
import osgi.enroute.dto.api.DTOsConstants;
import osgi.enroute.dto.api.IDTO;
import osgi.enroute.dto.api.TypeReference;

/**
 * This class provides utility functions for DTOs
 */
@ProvideCapability(ns=ImplementationNamespace.IMPLEMENTATION_NAMESPACE, name=DTOsConstants.DTOS_SPECIFICATION_NAME, version=DTOsConstants.DTOS_SPECIFICATION_VERSION)
@Component
public class DTOsProvider implements DTOs {
	private final static Field[]					EMPTY_FIELDS	= new Field[0];
	private final static JSONCodec					codec			= new JSONCodec();
	private final static Map<Class< ? >,Field[]>	cache			= Collections
																			.synchronizedMap(new WeakHashMap<Class< ? >,Field[]>());

	private final Link								root			= new Link(null, null, null);

	//
	// The link class is to keep track of cycles traversing and to
	// maintain the path at minimum cost.
	//

	static class Link {
		final Link		prev;
		final Object	object;
		final Object	name;

		public Link(Link link, Object name, Object object) {
			this.prev = link;
			this.name = name;
			this.object = object;
		}

		boolean isCycle(Object t) {
			if (this.object == t)
				return true;
			else if (prev == null)
				return false;
			else
				return prev.isCycle(t);
		}

		String[] getPath(int n) {
			if (prev == null) {
				String[] path = new String[n];
				return path;
			}
			String[] path = prev.getPath(n + 1);
			path[path.length - n - 1] = name.toString();
			return path;
		}

		void verifyCycle(Object o) {
			if (isCycle(o)) {
				throw new IllegalArgumentException("Cycle in DTO " + getPath(0));
			}
		}
	}

	//
	// Helper methods to return a Difference object
	// for diffing.
	//

	static class Diff extends Difference {
		public Diff(Reason reason, Link link) {
			this.reason = reason;
			this.path = link.getPath(0);
		}
	}

	//
	// Indicates a problem while traversing a path
	// or contains the result
	//

	static class Answer extends Retrieve {

		//
		//
		public Answer(Object dto, String failure) {
			value = dto;
			this.failure = failure;
		}

		public String toString() {
			if (failure != null)
				return "Fail: " + failure;
			else
				return "Value: " + value;
		}

		public boolean isFailure() {
			return failure != null;
		}

	}

	@Override
	public DTOs.Converter convert(final Object source) {
		return new DTOs.Converter() {
			public <T> T to(Class<T> dest) throws Exception {
				return aQute.lib.converter.Converter.cnv(dest, source);
			}

			@SuppressWarnings("unchecked")
			public <T> T to(TypeReference<T> dest) throws Exception {
				return (T) aQute.lib.converter.Converter.cnv(dest.getType(), source);
			}

			public Object to(Type dest) throws Exception {
				return aQute.lib.converter.Converter.cnv(dest, source);
			}
		};
	}

	@Override
	public Map<String,Object> asMap(Object dto) throws Exception {
		return new DTOMap(this, dto);
	}

	@Override
	public String toString(Object dto) {
		if (dto == null)
			return null + "";

		Field[] fields = getFields(dto);
		if (fields.length == 0)
			return dto.toString();

		try {
			try (Formatter format = new Formatter()) {
				for (Field f : fields) {
					format.format("%s: %s%n", f.getName(), f.get(dto));
				}
				return format.toString();
			}
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean equals(Object a, Object b) {
		try {
			return diff(a,b).isEmpty();
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public int hashCode(Object dto) {
		if (dto instanceof IDTO)
			return dto.hashCode();

		Field[] fields = getFields(dto);
		if (fields.length == 0)
			return dto.hashCode();

		int prime = 31;
		int result = 1;
		try {

			for (Field f : fields) {
				Object a = f.get(this);
				result = prime * result + (a == null ? 0 : hashCode(dto));
			}

			return result;
		}
		catch (Exception e) {
			return result;
		}
	}

	@Override
	public Answer get(Object dto, String path) throws Exception {
		return get(dto, fromPathToSegments(path));
	}

	@Override
	public Answer get(Object dto, String... path) throws Exception {
		return get(dto, path, 0, path.length);
	}

	private Answer get(Object dto, String[] path, int i, int max) throws Exception {
		if (i > path.length)
			throw new IllegalArgumentException("Incorrect index in path " + Arrays.toString(path) + "[" + i + "]");

		if (i == path.length || i == max)
			return new Answer(dto, null);

		if (dto == null)
			return new Answer(null, "null encountered @ " + Arrays.toString(path) + " " + i);

		String name = path[i];

		if (dto.getClass().isArray()) {
			int index = Integer.parseInt(name);
			if (index >= Array.getLength(dto))
				return new Answer(null, "path access contains an array but the corresponding index is not an integer: "
						+ Arrays.toString(path) + "[" + i + "]");

			return get(Array.get(dto, index), path, i + 1, max);
		}

		if (dto instanceof Collection) {
			Collection< ? > coll = (Collection< ? >) dto;
			int index = Integer.parseInt(name);
			if (index >= coll.size())
				return new Answer(null,
						"path access contains a collection but the corresponding index is not an integer: "
								+ Arrays.toString(path) + "[" + i + "]");

			if (coll instanceof List) {
				return get(((List< ? >) coll).get(index), path, i + 1, max);
			}
			for (Object o : coll) {
				if (index-- == 0)
					return get(o, path, i + 1, max);
			}
			assert false;
			return null; // unreachable
		}

		if (dto instanceof Map) {
			Object value = ((Map< ? , ? >) dto).get(name);
			return get(value, path, i + 1, max);
		}

		Field fields[] = getFields(dto);
		if (fields.length > 0) {
			for (Field field : fields) {
				if (field.getName().equals(name)) {
					return get(field.get(dto), path, i + 1, max);
				}
			}
		}

		return new Answer(null, "Unknown type to traverse " + dto.getClass() + " for " + name);
	}

	@Override
	public Answer set(Object dto, Object value, String path) throws Exception {
		return set(dto, value, fromPathToSegments(path));
	}

	@SuppressWarnings("unchecked")
	public Answer set(Object dto, Object value, String... path) throws Exception {
		try {
			if (path.length == 0)
				throw new IllegalArgumentException(
						"To set a value, you need at least one path segment, this one is empty ");

			Answer target = get(dto, path, 0, path.length - 1);
			if (target.isFailure())
				return target;

			String name = path[path.length - 1];

			if (dto instanceof Collection) {
				Collection<Object> coll = (Collection<Object>) dto;
				switch (name) {
					case "+" :
						coll.add(value);
						break;
					case "-" :
						coll.add(value);
						break;

					default :
						int index = Integer.parseInt(name);
						if (coll instanceof List) {
							List<Object> l = (List<Object>) coll;
							while (l.size() < index + 1)
								l.add(null);
							l.set(index, value);
							return new Answer(dto, null);
						}
						return new Answer(null, "Cannot " + Arrays.toString(path) + "[" + index + "]");
				}
				return new Answer(null, "Only List can be indexed, is " + coll.getClass());
			}

			if (dto.getClass().isArray()) {
				int index = Integer.parseInt(name);
				if (index >= Array.getLength(dto))
					return new Answer(null,
							"path access contains an array but the corresponding index is not an integer: "
									+ Arrays.toString(path) + "[" + index + "]");

				Array.set(dto, index, value);
				return new Answer(dto, null);
			}

			if (dto instanceof Map) {
				Map<Object,Object> map = (Map<Object,Object>) dto;
				map.put(name, value);
				return new Answer(dto, null);
			}

			Field fields[] = getFields(dto);
			if (fields.length > 0) {
				for (Field field : fields) {
					if (field.getName().equals(name)) {
						field.set(dto, value);
						return new Answer(dto, null);
					}
				}
			}
			return new Answer(null, "Unknown type to set value for " + dto.getClass());
		}
		catch (final Exception e) {
			return new Answer(null, e.getMessage());
		}
	}

	@Override
	public List<Difference> diff(Object older, final Object newer) throws Exception {
		List<Difference> diffs = new ArrayList<>();
		diff(diffs, root, older, newer);
		return diffs;
	}

	private boolean diff(List<Difference> diffs, Link link, Object older, Object newer) throws Exception {
		if (older == newer)
			return false;

		if (older == null) {
			diffs.add(new Diff(Reason.ADDED, link));
			return true;
		}

		if (newer == null) {
			diffs.add(new Diff(Reason.REMOVED, link));
			return true;
		}

		Class< ? > oc = older.getClass();
		Class< ? > nc = newer.getClass();
		if (oc != nc) {
			diffs.add(new Diff(Reason.DIFFERENT_TYPES, link));
			return true;
		}

		if (older.equals(newer))
			return false;

		if (older instanceof Collection< ? >) {
			Collection< ? > co = (Collection< ? >) older;
			Collection< ? > cn = (Collection< ? >) newer;

			if (co.size() != cn.size()) {
				diffs.add(new Diff(Reason.SIZE, link));
				return true;
			}

			if (co.equals(cn))
				return false;

			//
			// They're different, if it is a list we can find out which
			//

			if (older instanceof List< ? >) {
				List< ? > clo = (List< ? >) older;
				List< ? > cln = (List< ? >) newer;

				for (int i = 0; i < co.size(); i++) {
					Object lo = clo.get(i);
					Object ln = cln.get(i);
					diff(diffs, new Link(link, i, older), lo, ln);
				}
				return true;
			}

			//
			// If not a list, we're lost ...
			//

			diffs.add(new Diff(Reason.UNEQUAL, link));
			return true;
		}

		if (oc.isArray()) {
			Object[] ao = new Object[] {
				older
			};
			Object[] an = new Object[] {
				newer
			};
			if (Arrays.deepEquals(ao, an)) {
				return false;
			}

			int lo = Array.getLength(older);
			int ln = Array.getLength(newer);
			if (lo != ln) {
				diffs.add(new Diff(Reason.SIZE, link));
				return true;
			}

			for (int i = 0; i < lo; i++) {
				diff(diffs, new Link(link, i, older), Array.get(older, i), Array.get(newer, i));
			}
			return true;
		}

		if (older instanceof Map< ? , ? >) {
			Map< ? , ? > co = (Map< ? , ? >) older;
			Map< ? , ? > cn = (Map< ? , ? >) newer;

			if (co.size() != cn.size()) {
				diffs.add(new Diff(Reason.SIZE, link));
				return true;
			}

			if (co.equals(cn))
				return false;

			if (!co.keySet().equals(cn.keySet())) {
				diffs.add(new Diff(Reason.KEYS, link));
				return true;
			}

			for (Map.Entry< ? , ? > e : co.entrySet()) {
				Object key = e.getKey();
				if (!(key instanceof String)) {
					diffs.add(new Diff(Reason.NO_STRING_MAP, link));
					return true;
				}

				String k = escape((String) key);

				Object no = co.get(key);
				Object nn = cn.get(key);

				diff(diffs, new Link(link, k, older), no, nn);
			}
			return true;
		}

		Field[] fields = getFields(older);
		if (fields.length > 0) {
			for (Field field : fields) {
				Object o = field.get(older);
				Object n = field.get(newer);
				diff(diffs, new Link(link, field.getName(), older), o, n);
			}
			return true;
		}

		diffs.add(new Diff(Reason.UNEQUAL, link));
		return true;
	}

	static Pattern	ESCAPE_P	= Pattern.compile("(\\.|\\\\)");
	static Pattern	UNESCAPE_P	= Pattern.compile("\\\\(\\.|\\\\)");

	public String escape(String unescaped) {
		return ESCAPE_P.matcher(unescaped).replaceAll("\\\\$1");
	}

	public String unescape(String unescaped) {
		return UNESCAPE_P.matcher(unescaped).replaceAll("$1");
	}

	public boolean isComplex(Object a) {
		return a != null
				&& (a instanceof Map || a instanceof Collection || a instanceof DTO || a.getClass().isArray() || getFields(a).length > 0);
	}

	public boolean isDTO(Object o) {
		return getFields(o).length != 0;
	}

	class EncImpl implements Enc {
		private Encoder	enc	= codec.enc();
		private Object	source;

		public EncImpl(Object source) {
			this.source = source;
		}

		@Override
		public void put(OutputStream out) throws Exception {
			this.put(out, "UTF-8");
		}

		@Override
		public void put(OutputStream out, String charset) throws Exception {
			enc.charset(charset).to(out).put(source);
		}

		@Override
		public void put(Appendable out) throws Exception {
			enc.to(out).put(source);
		}

		@Override
		public String put() throws Exception {
			return enc.to().put(source).toString();
		}

		@Override
		public Enc pretty() {
			enc.indent("\t");
			return this;
		}

		@Override
		public Enc ignoreNull() {
			return this;
		}

	}

	@SuppressWarnings("unchecked")
	class DecImpl<T> implements Dec<T> {
		private Decoder	dec	= codec.dec();
		private Type	type;

		public DecImpl(Type type) {
			this.type = type;
		}

		@Override
		public T get(InputStream in) throws Exception {
			return (T) dec.charset("UTF-8").from(in).get(type);
		}

		@Override
		public T get(InputStream in, String charset) throws Exception {
			return (T) dec.charset(charset).from(in).get(type);
		}

		@Override
		public T get(Reader in) throws Exception {
			return (T) dec.from(in).get(type);
		}

		@Override
		public T get(CharSequence in) throws Exception {
			return (T) dec.charset("UTF-8").from(in.toString()).get(type);
		}

	}

	@Override
	public Enc encoder(Object source) throws Exception {
		return new EncImpl(source);
	}

	@Override
	public <T> Dec<T> decoder(Class<T> type) throws Exception {
		return new DecImpl<T>(type);
	}

	@Override
	public <T> Dec<T> decoder(TypeReference<T> type) throws Exception {
		return new DecImpl<T>(type.getType());
	}

	@Override
	public Dec< ? > decoder(Type type, InputStream source) throws Exception {
		return new DecImpl<Object>(type);
	}

	Field[] getFields(Object o) {
		if (o == null)
			return EMPTY_FIELDS;
		return getFields(o.getClass());
	}

	Field[] getFields(Class< ? > c) {
		Field fields[] = cache.get(c);
		if (fields == null) {
			List<Field> publicFields = new ArrayList<>();

			for (Field field : c.getFields()) {
				if (field.isEnumConstant() || field.isSynthetic() || Modifier.isStatic(field.getModifiers()))
					continue;
				publicFields.add(field);
			}
			Collections.sort(publicFields, new Comparator<Field>() {

				@Override
				public int compare(Field o1, Field o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});

			cache.put(c.getClass(), fields = publicFields.toArray(new Field[publicFields.size()]));
		}
		return fields;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Comparator<T> getComparator(Class<T> dtoClass) {
		Field[] fields = getFields(dtoClass);
		if (fields.length == 0)
			return null;

		List<Comparator<T>> comparables = new ArrayList<>();

		for (final Field f : fields) {
			if (Comparable.class.isAssignableFrom(f.getType())) {
				comparables.add(new Comparator<T>() {
					@Override
					public int compare(T o1, T o2) {
						try {
							Comparable<Object> oo1 = (Comparable<Object>) f.get(o1);
							Comparable<Object> oo2 = (Comparable<Object>) f.get(o2);
							return oo1.compareTo(oo2);
						}
						catch (Exception e) {
							// cannot happen since we only look at public fields
							return 0;
						}
					}

				});
			} else {
				final Comparator<Object> comparator = (Comparator<Object>) getComparator(f.getType());
				if (comparator != null) {
					comparables.add(new Comparator<T>() {
						@Override
						public int compare(T o1, T o2) {
							try {
								Object oo1 = f.get(o1);
								Object oo2 = f.get(o2);
								return comparator.compare(oo1, oo2);
							}
							catch (Exception e) {
								// cannot happen since we only look at public
								// fields
								return 0;
							}
						}

					});
				}
			}
		}

		//
		// Check if we found any fields to sort on
		//
		if (comparables.isEmpty())
			return null;

		final Comparator<T> comparators[] = comparables.toArray(new Comparator[comparables.size()]);

		return new Comparator<T>() {
			@Override
			public int compare(T a, T b) {
				try {
					for (Comparator<T> c : comparators) {
						int result = c.compare(a, b);
						if (result != 0)
							return result;
					}
					return 0;
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	Field getField(Field[] fields, String name) {
		int index = bsearch(fields, 0, fields.length, name);
		if (index < 0)
			return null;
		else
			return fields[index];
	}

	int bsearch(Field[] a, int fromIndex, int toIndex, String key) {
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			Field midVal = a[mid];
			int cmp = midVal.getName().compareTo(key);
			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1); // key not found.
	}

	/**
	 * Shallow copy
	 */

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	@Override
	public <T> T shallowCopy(T source) throws Exception {
		if (!isComplex(source))
			return source;

		Class<T> c = (Class<T>) source.getClass();

		if (c.isArray()) {
			int l = Array.getLength(source);
			T dest = (T) Array.newInstance(c.getComponentType(), l);
			System.arraycopy(source, 0, dest, 0, l);
			return dest;
		}

		T dest = c.newInstance();

		if (source instanceof Map) {
			((Map) dest).putAll((Map) source);
			return dest;
		}

		if (source instanceof Collection) {
			((Collection) dest).addAll((Collection) source);
			return dest;
		}

		for (Field field : getFields(c)) {
			field.set(dest, field.get(source));
		}
		return dest;
	}

	/**
	 * Deep copy
	 */

	public <T> T deepCopy(T source) throws Exception {
		return deepCopy(source, root);
	}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	<T> T deepCopy(T source, Link link) throws Exception {
		if (!isComplex(source))
			return source;

		link.verifyCycle(source);

		Class<T> c = (Class<T>) source.getClass();

		if (c.isArray()) {
			int l = Array.getLength(source);
			T dest = (T) Array.newInstance(c.getComponentType(), l);

			for (int i = 0; i < l; i++) {
				Object s = Array.get(source, i);
				Array.set(dest, i, deepCopy(s, new Link(link, i, source)));
			}
			return dest;
		}

		T dest = c.newInstance();

		if (source instanceof Map) {
			Map<Object,Object> d = (Map<Object,Object>) dest;
			Map<Object,Object> s = (Map<Object,Object>) source;
			for (Entry< ? , ? > entry : s.entrySet()) {
				Link next = new Link(link, entry.getKey(), source);
				d.put(deepCopy(entry.getKey(), next), deepCopy(entry.getValue(), next));
			}
			return dest;
		}

		if (source instanceof Collection) {
			Collection s = (Collection) source;
			Collection d = (Collection) dest;
			int i = 0;
			for (Object o : s) {
				Link next = new Link(link, i++, source);
				d.add(deepCopy(o, next));
			}
			return dest;
		}

		for (Field field : getFields(c)) {
			Link next = new Link(link, field.getName(), source);
			field.set(dest, deepCopy(field.get(source), next));
		}
		return dest;
	}

	public String[] fromPathToSegments(String path) {
		return fromPathToSegments(path, 0, 0);
	}

	String[] fromPathToSegments(String path, int start, int n) {
		if (start >= path.length()) {
			return new String[n];
		}

		StringBuilder sb = new StringBuilder();
		int i = start;
		outer: for (; i < path.length(); i++) {
			char c = path.charAt(i);
			switch (c) {

				case '.' :
					break outer;

				case '\\' :
					c = path.charAt(++i);
					assert c == '.' || c == '\\';

				default :
					sb.append(c);
					break;
			}
		}
		String[] result = fromPathToSegments(path, i + 1, n + 1);
		result[n] = sb.toString();
		return result;
	}

	@Override
	public String fromSegmentsToPath(String[] segments) {
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (String segment : segments) {
			sb.append(del);
			for (int i = 0; i < segment.length(); i++) {
				char c = segment.charAt(i);
				switch (c) {
					case '\\' :
					case '.' :
						sb.append('\\');

						// FALL THROUGH
						
					default :
						sb.append(c);
						break;
				}
			}
			del = ".";
		}
		return sb.toString();
	}

	@Override
	public boolean deepEquals(Object a, Object b) {
		try {
			return diff(a,b).isEmpty();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isValidDTO(Object o) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}
}
