package osgi.enroute.dto.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.dto.DTO;

/**
 * This service provides a number of utilities to make it easy to work with
 * DTOs. It contains a number of utility functions.
 * <p>
 * DTOs recognize primary fields (annotated with {@link PrimaryKey}). If none of
 * the fields is annotated, all fields are primary.
 */
@ProviderType
public interface DTOs {

	/**
	 * Converts an object to another type
	 */
	interface Converter {
		/**
		 * Convert an object source to the type given in dest. If the conversion
		 * cannot be done, return null. If the destination type is an interface
		 * then an attempt is made to detect an implementation class. This works
		 * well for all the collection interfaces.
		 * <p>
		 * The conversion uses the type information and any generic information
		 * to do a proper conversion. I.e. a {@code List<Integer>} can be
		 * converted to a {@code String[]} and vice versa. It also recognizes
		 * {@link DTO}s and allows them to be converted to maps and vice versa.
		 * Maps can also be converted to interfaces, the names of the methods
		 * then act as keys in the map. Same conventions for name mangling are
		 * used as the DS configuration properties
		 * 
		 * @param dest
		 *            The destination type.
		 * @return a converted object implementing/extending T
		 * @throws Exception
		 */
		<T> T to(Class<T> dest) throws Exception;

		/**
		 * Convert an object to the given type reference.
		 * 
		 * @param dest
		 *            the destination type specification
		 * @return the converted object or {@code null} if no conversion could
		 *         be found.
		 */
		@SuppressWarnings("javadoc")
		<T> T to(TypeReference<T> dest) throws Exception;

		/**
		 * Convert an object to the given type. This type can be any of the
		 * given implementers of type, like ParameterizedType, etc. The
		 * converter must be able to follow these generic chains.
		 * 
		 * @param dest
		 *            the destination type
		 * @return the converted object or null if no conversion could be found.
		 * @throws Exception
		 */
		Object to(Type dest) throws Exception;
	}

	/**
	 * Create a converter on an object.
	 * 
	 * @param source
	 *            The source to convert, {@code null} is allowed
	 * @return a converter for the given source
	 * @throws Exception
	 */
	Converter convert(Object source) throws Exception;

	/**
	 * Return a partially read only Map object that maps directly to a DTO. I.e.
	 * changes are reflected in the DTO. If a field is a DTO, then this field
	 * will also become a Map.
	 * 
	 * @param dto
	 *            the DTO
	 * @return a Map where the keys map to the field names and the values to the
	 *         field values. This map is not modifiable.
	 * @throws Exception
	 */
	Map<String,Object> asMap(Object dto) throws Exception;

	/**
	 * This interface is a builder for encoding a DTO to JSON.
	 */
	interface Enc {
		void put(OutputStream out) throws Exception;

		void put(OutputStream out, String charset) throws Exception;

		void put(Appendable out) throws Exception;

		String put() throws Exception;

		Enc pretty();

		Enc ignoreNull();
	}

	/**
	 * Return encoder builder for JSON
	 * 
	 * @param source
	 *            the object to encode
	 * @return a builder to control the encoding
	 * @throws Exception
	 */
	Enc encoder(Object source) throws Exception;

	/**
	 * An interface to control the building of a JSON decoder.
	 * 
	 * @param <T>
	 *            the type to decode to
	 */
	interface Dec<T> {
		/**
		 * Decode JSON from an input stream with UTF-8 encoding
		 * 
		 * @param in
		 *            the input stream
		 * @return a JSON decoded object
		 * @throws Exception
		 */
		T get(InputStream in) throws Exception;

		/**
		 * Decode JSON from an input stream and specify the character set to b
		 * used.
		 * 
		 * @param in
		 *            the input stream
		 * @param charset
		 *            The character set to use
		 * @return a JSON decoded object
		 * @throws Exception
		 */
		T get(InputStream in, String charset) throws Exception;

		/**
		 * Decode JSON from a reader
		 * 
		 * @param in
		 *            the reader
		 * @return a JSON decoded object
		 * @throws Exception
		 */
		T get(Reader in) throws Exception;

		/**
		 * Decode JSON from a a CharSequence
		 * 
		 * @param in
		 *            the Char Sequence
		 * @return a JSON decoded object
		 * @throws Exception
		 */
		T get(CharSequence in) throws Exception;
	}

	/**
	 * Return a JSON decoder that uses the type to control the parsing.
	 * 
	 * @param type
	 *            the type that controls actual types for parsing
	 * @return a decoder
	 * @throws Exception
	 */
	<T> Dec<T> decoder(Class<T> type) throws Exception;

	/**
	 * Return a JSON decoder that uses the type to control the parsing.
	 * 
	 * @param type
	 *            the type that controls actual types for parsing
	 * @return a decoder
	 * @throws Exception
	 */
	<T> Dec<T> decoder(TypeReference<T> type) throws Exception;

	/**
	 * Return a JSON decoder on an input stream that uses the type to control
	 * the parsing.
	 * 
	 * @param type
	 *            the type that controls actual types for parsing
	 * @param source
	 *            the source of the JSON
	 * @return a decoder
	 * @throws Exception
	 */
	Dec< ? > decoder(Type type, InputStream source) throws Exception;

	/**
	 * Convert a DTO to a human readable string presentation. This is primarily
	 * for debugging since the toString can truncate fields. This method must
	 * print all public fields, also non primary. Output formats can vary (e.g.
	 * YAML like) so the actual output should NOT be treated as standard.
	 * 
	 * @param dto
	 *            the dto to turn into a string
	 * @return a human readable string (not json!)
	 */
	String toString(Object dto);

	/**
	 * Check if two dtos fields are equal. This is shallow equal, that is the
	 * fields of this DTO are using the equals() instance method.
	 * 
	 * @param a
	 *            the first object
	 * @param b
	 *            the second object
	 * @return true if both are null or the DTO's primary fields are equal
	 */
	boolean equals(Object a, Object b);

	/**
	 * Check if two DTOs fields are equal. This is deep equal, that is the
	 * fields of this DTO are using this method is the object at a field is a
	 * DTO, recursively.
	 * 
	 * @param a
	 *            the first object
	 * @param b
	 *            the second object
	 * @return true if both are null or the DTO's primary fields are equal
	 */
	boolean deepEquals(Object a, Object b);

	/**
	 * Calculate a hash Code for the fields in this DTO. The dto must have at
	 * least one public field.
	 * 
	 * @param dto
	 *            the object to calculate the hashcode for, must not be null .
	 * @return a hashcode
	 */
	int hashCode(Object dto);

	/**
	 * Access a DTO with a path. A path is a '.' separated string. Each part in
	 * the path is either a field name, key in a map, or an index in a list. If
	 * the path segments contain dots or backslashes, then these must be escaped
	 * 
	 * @param dto
	 *            the root
	 * @param path
	 *            the path, should only contain dots as separators
	 * @return the value of the object or null if not found.
	 * @throws Exception
	 */

	Retrieve get(Object dto, String path) throws Exception;

	/**
	 * Access a DTO with a path that consists of an array with segments. Each
	 * segment in the path is either a field name, key in a map, or an index in
	 * a list.
	 * 
	 * @param dto
	 *            the root
	 * @param path
	 *            the path
	 * @return the value of the object or null if not found.
	 * @throws Exception
	 */
	Retrieve get(Object dto, String... path) throws Exception;

	/**
	 * Access a DTO with a path. A path is a '.' separated string. Each part in
	 * the path is either a field name, key in a map, or an index in a list.
	 * Maps and Lists are extended to hold the new value. This method does not
	 * support dots in the path. For paths with dots in the keys, use
	 * {@link #get(Object, String...)}.
	 * 
	 * @param dto
	 *            the root
	 * @param value
	 *            the value to set
	 * @param path
	 *            the path, should only contain dots as separators
	 * @return the value of the object or null if not found.
	 * @throws Exception
	 */
	Retrieve set(Object dto, Object value, String path) throws Exception;

	/**
	 * Set a value in a DTO with a path that consists of an array with segments.
	 * Each segment in the path is either a field name, key in a map, or an
	 * index in a list.
	 * 
	 * @param dto
	 *            the root
	 * @param value
	 *            the value to set
	 * @param path
	 *            the path
	 * @return the value of the object or null if not found.
	 * @throws Exception
	 */
	Retrieve set(Object dto, Object value, String... path) throws Exception;

	/**
	 * The DTO that contains the result of a path traversal.
	 */
	class Retrieve {
		/**
		 * Is set when the path could not be traversed. The value is the reason
		 * of the failure.
		 */
		public String failure;

		/**
		 * For a set, this returns the old value, for a get, this return the
		 * value.
		 */
		public Object value;
	}

	/**
	 * Return a comparator who works on the primary fields of a DTO class.
	 * 
	 * @param dtoClass
	 *            the dto class
	 * @return a comparator for the given DTO or null if the dtoClass has no
	 *         fields of a comparable type. If a field has public fields and
	 *         these are comparable then they are also included recursively.
	 */
	<T> Comparator<T> getComparator(Class<T> dtoClass);

	/**
	 * Return a list of paths where the two objects differ. The objects must be
	 * of the same class.
	 * 
	 * @param older
	 *            the older object
	 * @param newer
	 *            the newer object
	 * @return A list of differences, if there is no difference, the list is
	 *         empty.
	 * @throws Exception
	 */
	List<Difference> diff(Object older, Object newer) throws Exception;

	/**
	 * The details of a difference
	 */
	class Difference extends DTO {
		/**
		 * The path where there was a difference
		 */
		public String path[];

		/**
		 * The reason why there was a difference
		 */
		public Reason reason;
	}

	/**
	 * The reason for a difference.
	 */
	enum Reason {
		UNEQUAL, REMOVED, ADDED, DIFFERENT_TYPES, SIZE, KEYS, NO_STRING_MAP, INVALID_KEY;
	}

	/**
	 * Takes a path with escaped '.'and '\' and then turns it into an array of
	 * unescaped keys
	 * 
	 * @param path
	 *            the path with escaped \ and .
	 * @return a path array with unescaped segments
	 */
	String[] fromPathToSegments(String path);

	/**
	 * Takes a path with unescaped keys and turns it into a string path where
	 * the \ and . are escaped.
	 * 
	 * @param segments
	 *            The unescaped segments of the path
	 * @return a string path where the . and \ are escaped.
	 */
	String fromSegmentsToPath(String[] segments);

	/**
	 * Escape a string to be used in a path. This will put a backslash ('\') in
	 * front of full stops ('.') and the backslash ('\').
	 * 
	 * @param unescaped
	 *            the string to be escaped
	 * @return a string where all '.' and '\' are escaped with a '\'.
	 */
	String escape(String unescaped);

	/**
	 * Unescapes a string to be used in a path. This will remove a backslash
	 * ('\') in front of full stops ('.') and the backslash ('\').
	 * 
	 * @param escaped
	 *            the string to be unescaped
	 * @return a string where all '\.' and '\\' have the preceding backslash
	 *         removed with a '\'.
	 */
	String unescape(String escaped);

	/**
	 * Return true if the give dto is complex (either Map, Collection, Array, or
	 * has public fields.
	 * 
	 * @param object
	 *            The DTO to check
	 * @return <code>true</code> if this is a DTO with fields or length.
	 */

	boolean isComplex(Object object);

	/**
	 * An object with public fields.
	 * 
	 * @param dto
	 *            the object to check
	 * @return true if this object has public fields or extends DTO
	 */
	boolean isDTO(Object dto);

	/**
	 * Verify that the object is a DTO and has no cycles.
	 * 
	 * @param o
	 *            The object to check for no cycles
	 * @return {@code true} if this DTO has no cycles and is further ok
	 * @throws Exception
	 */

	boolean isValidDTO(Object o) throws Exception;

	/**
	 * Create a shallow copy of a DTO. This will create a new object of the same
	 * type and copy the public fields of the source to the new copy. It will
	 * not create a copy for these values.
	 * 
	 * @param object
	 *            the source object
	 * @return a shallow copy of object
	 * @throws Exception
	 */

	<T> T shallowCopy(T object) throws Exception;

	/**
	 * Create a deep copy of a DTO. This will copy the fields of the DTO. Copied
	 * values will also be created anew if they are complex (Map, Collection,
	 * DTO, or Array). Other objects are assumed to immutable unless they
	 * implement Cloneable.
	 * 
	 * @param object
	 *            the object to deep copy
	 * @return the deep copied object
	 * @throws Exception
	 */

	<T> T deepCopy(T object) throws Exception;

	/**
	 * A map that is to set inline like a builder.
	 * 
	 * @param <K>
	 *            the key type
	 * @param <V>
	 *            the value type
	 */
	class LiteralMap<K, V> extends LinkedHashMap<K,V> {
		private static final long serialVersionUID = 1L;

		LiteralMap<K,V> set(K key, V value) {
			put(key, value);
			return this;
		}
	}

	/**
	 * Convenience method to create a map that returns itself when set so it can
	 * be used as a builder.
	 * 
	 * @param key
	 *            the initial key
	 * @param value
	 *            the initial value
	 * @return the map
	 */
	default <K, V> LiteralMap<K,V> map(K key, V value) {
		return new LiteralMap<K,V>().set(key, value);
	}

}
