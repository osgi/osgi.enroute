package osgi.enroute.quantity.base.util;

import java.util.Optional;
import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;

/**
 * Base class for converters. Contains a to and from lambda.
 * @param <T>
 */
public class AbstractConverter<T> implements Converter<T> {
	DoubleFunction<T>	toType;
	ToDoubleFunction<T>	fromType;
	Class<T>			type;
	Optional<String>	suffix;

	public AbstractConverter(Class<T> type, DoubleFunction<T> toType, ToDoubleFunction<T> fromType, String suffix) {
		this.type = type;
		this.toType = toType;
		this.fromType = fromType;
		this.suffix = suffix == null ? Optional.empty() : Optional.of(suffix);
	}

	@Override
	public T toType(double value) throws Exception {
		return toType.apply(value);
	}

	@Override
	public double fromType(T value) throws Exception {
		return fromType.applyAsDouble(value);
	}

	@Override
	public Optional<String> getSuffix() {
		return suffix;
	}
}
