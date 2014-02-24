package osgi.enroute.dto.api;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * This service provides a number of utilities to make it easy to work with
 * DTOs. It contains a number of utility functions.
 * <p>
 * DTOs recognize primary fields (annotated with {@link Primary}). If none of
 * the fields is annotated, all fields are primary.
 */
public interface DTOs {

	/**
	 * Convert an object source to the type given in dest. If the conversion
	 * cannot be done, return null. If the destination type is an interface then
	 * an attempt is made to detect an implementation class. This works well for
	 * all the collection interfaces.
	 * <p>
	 * The conversion uses the type information and any generic information to
	 * do a proper conversion. I.e. a {@code List<Integer>} can be converted to
	 * a {@code String[]} and vice versa. It also recognizes DTOs and allows
	 * them to be converted to maps and vice versa. Maps can also be converted
	 * to interfaces, the names of the methods then act as keys in the map. Same
	 * conventions for name mangling are used as the DS configuration properties
	 * <p>
	 * TODO wait for R6 to link this
	 * 
	 * @param dest
	 *            The destination type.
	 * @param source
	 *            The source object to be converted. Must not be null.
	 * @return a converted object that can
	 */
	<T> T convert(Class<T> dest, Object source) throws Exception;

	/**
	 * Convert an object to the given type reference.For more rules, see
	 * {@link #convert(Class, Object)}.
	 * 
	 * @param dest
	 *            the destination type specification
	 * @param source
	 *            the source object, must not be null
	 * @return the converted object or null if no conversion could be found.
	 */
	<T> T convert(TypeReference<T> dest, Object source) throws Exception;
	/**
	 * Convert an object to the given type. This type can be any of the given
	 * implementers of type, like ParameterizedType, etc. The converter must be
	 * able to follow these generic chains. For more rules, see
	 * {@link #convert(Class, Object)}.
	 * 
	 * @param dest
	 *            the destination type
	 * @param source
	 *            the source object, must not be null
	 * @return the converted object or null if no conversion could be found.
	 */
	Object convert(Type dest, Object source) throws Exception;

	/**
	 * Return a Map object that maps directly to a DTO.
	 * 
	 * @param dto
	 *            the DTO (must have public fields)
	 * @return a Map where the keys map to the field names and the values to the
	 *         field values. This map is not modifiable.
	 */
	Map<String,Object> asMap(Object dto) throws Exception;

	/**
	 * Return a JSON codec.
	 * 
	 * @return a JSON codec
	 */
	Codec json();

	/**
	 * Convert a DTO to a human readable string presentation. This is primarily
	 * for debugging since the toString can truncate fields. This method must
	 * print all public fields, also non primary.
	 * 
	 * @param dto
	 *            the dto to turn into a string
	 * @return
	 */
	String toString(Object dto);

	/**
	 * Check if two dtos primary fields are equal.
	 * 
	 * @param a
	 * @param b
	 * @return true if both are null or the DTO's primary fields are equal
	 */
	boolean equals(Object a, Object b);

	/**
	 * Calculate a hash Code for the primary fields.
	 * 
	 * @param dto
	 *            the object to calculate the hashcode for, must not be null.
	 * @return a hashcode
	 */
	int hashCode(Object dto);

	/**
	 * Access a DTO with a path. A path is a '.' separated string. Each part in
	 * the path is either a field name, key in a map, or an index in a list.
	 * This method does not support dots in the path. For paths with dots in the
	 * keys, use {@link #get(Object, String...)}.
	 * 
	 * @param dto
	 *            the root
	 * @param path
	 *            the path, should only contain dots as separators
	 * @return the value of the object or null if not found.
	 */
	Object get(Object dto, String path) throws Exception;

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
	 */
	Object get(Object dto, String... path) throws Exception;

	/**
	 * Access a DTO with a path. A path is a '.' separated string. Each part in
	 * the path is either a field name, key in a map, or an index in a list.
	 * Maps and Lists are extended to hold the new value. This method does not
	 * support dots in the path. For paths with dots in the keys, use
	 * {@link #get(Object, String...)}.
	 * 
	 * @param dto
	 *            the root
	 * @param path
	 *            the path, should only contain dots as separators
	 * @return the value of the object or null if not found.
	 */
	void set(Object dto, Object value, String path) throws Exception;

	/**
	 * Set a value in a DTO with a path that consists of an array with segments.
	 * Each segment in the path is either a field name, key in a map, or an
	 * index in a list.
	 * 
	 * @param dto
	 *            the root
	 * @param value
	 *            TODO
	 * @param path
	 *            the path
	 * @return the value of the object or null if not found.
	 */
	void set(Object dto, Object value, String... path) throws Exception;

	/**
	 * The implementation of this service maintains a cache of the primary
	 * services of a DTO. If not field is annotated as primary, all fields are
	 * considered primary.
	 * 
	 * @param dto
	 *            the primary fields.
	 * @return the primary fields
	 */
	Field[] getPrimaryFields(Object dto);

	/**
	 * Return a comparator who works on the primary fields of a DTO class.
	 * 
	 * @param dtoClass
	 *            the dto class
	 * @return a comparator for the given DTO
	 */
	<T> Comparator<T> getComparator(Class<T> dtoClass);

	/**
	 * Return a list of paths where the two objects differ. The objects must be
	 * of the same class.
	 * 
	 * @param a
	 * @param b
	 * @return null if a and b are equal, otherwise a list of paths that are
	 *         useful in {@link #get(Object, String...)} and
	 *         {@link #set(Object, Object, String...)}
	 */
	List<String[]> diff(Object a, Object b);

}
