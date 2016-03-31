package osgi.enroute.quantity.base.util;

import java.util.Optional;

public interface Converter<T> {
	T toType(double value) throws Exception;

	double fromType(T value) throws Exception;

	Optional<String> getSuffix();

}
