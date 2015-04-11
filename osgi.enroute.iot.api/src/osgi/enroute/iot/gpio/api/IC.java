package osgi.enroute.iot.gpio.api;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 
 */
public abstract class IC<Input, Output> {
	private Output out;
	private final Map<String, Pin<Object>> outputs = new HashMap<String, Pin<Object>>();
	private Class<Output> output;
	private Class<Input> input;

	public IC() {
		Class<?> rover = this.getClass();
		while (rover.getSuperclass() != IC.class) {
			rover = rover.getSuperclass();
		}
		ParameterizedType zuper = (ParameterizedType) rover
				.getGenericSuperclass();
		this.input = resolve(zuper.getActualTypeArguments()[0], "input");
		this.output = resolve(zuper.getActualTypeArguments()[1], "output");
	}

	@SuppressWarnings("unchecked")
	private <T> Class<T> resolve(Type type, String msg) {
		if (type instanceof Class) {
			return (Class<T>) type;
		}
		throw new RuntimeException(
				"The "
						+ msg
						+ " type variable is not a class. You must use a concrete interface or class without wildcards, variables, arrays. or parameterized types");
	}

	public void connect(String name, Pin<Object> pin) {
		outputs.put(name, pin);
	}

	@SuppressWarnings("unchecked")
	public synchronized Output out() {
		if (out == null) {
			out = (Output) Proxy.newProxyInstance(output.getClass()
					.getClassLoader(), new Class<?>[] { output },
					new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method,
								Object[] args) throws Throwable {
							if (args.length != 1)
								return null;

							if (args[0] == null)
								return null;

							Pin<Object> pin = outputs.get(method.getName());
							if (pin != null)
								pin.set(args[0]);
							return null;
						}
					});
		}
		return out;
	}

	public List<String> getInputs() {
		return names(input);
	}

	public List<String> getOutputs() {
		return names(output);
	}

	private List<String> names(Class<?> c) {
		return Stream.of(c.getMethods()).map((m) -> m.getName())
				.collect(Collectors.toList());
	}
}
