package osgi.enroute.dto.api;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * An object does not carry any runtime information about its generic type.
 * However sometimes it is necessary to specify a generic type, that is the
 * purpose of this class. It allows you to specify an generic type by defining a
 * type T, then subclassing it. The subclass will have a reference to the super
 * class that contains this generic information. Through reflection, we pick
 * this reference up and return it with the getType() call.
 * 
 * <pre>
 * 	List<String> result = dtos.convert( new TypeReference<List<String>() {}, Arrays.asList(1,2,3));
 * </pre>
 * 
 * @param <T>
 */
public class TypeReference<T> {

	protected TypeReference() {
		// Make sure it cannot be directly instantiated
		// but it can be extended (that is the whole idea)
	}

	/**
	 * Return the actual type of this Type Reference
	 * 
	 * @return the type of this reference.
	 */
	public Type getType() {
		return ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}
}
