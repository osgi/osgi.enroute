package osgi.enroute.iot.gpio.api;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An IC (from Integrated Circuit) is a component with n inputs and m outputs.
 * The inputs & outputs of an IC are typed by an interface. This interface must
 * have <em>IO</em> methods with simple names that would match names on an IC.
 * This method must have a void or boolean return type. It must take a single
 * parameter of any scalar or DTO type. Surprisingly, this method prototype
 * pattern is for the inputs and the outputs. Since inputs and outputs are
 * fundamentally different, this requires some explanation.
 * <p>
 * The purpose of the IC is to allow them to be wired with an external
 * configurator. This configurator can be a GUI that connects the named outputs
 * from one IC to the named inputs of another (or the same) IC. The configurator
 * will signal the IC when the signal changes (or when it feels like it).
 * <p>
 * If the IC has a change in signal (or just feels like it) it can call a proxy
 * with the {@link #out()} method. This proxy implements the Output interface
 * and thus has all the methods to set the output.
 * <p>
 * The configurator can connect to the output pins by calling
 * {@link #connect(Output)} and should call disconnect before it goes away. The
 * IC class will call any output on all registered Output objects.
 */
public abstract class IC<Input, Output> {
	private final CopyOnWriteArrayList<Output> outputs = new CopyOnWriteArrayList<Output>();
	private final Class<Output> output;
	private final Class<Input> input;
	private Output out = null;

	/**
	 * Constructor.
	 */
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

	/**
	 * Connect the output and flush all output pins to this output object. After
	 * this call output changes will update the output object.
	 * 
	 * @param output
	 *            the output object
	 */
	public void connect(Output output) throws Exception {
		synchronized (outputs) {
			outputs.add(output);
			flush(output);
		}
	}

	/**
	 * Disconnect the output object from this IC.
	 * 
	 * @param output
	 *            the output object
	 */
	public void disconnect(Output output) throws Exception {
		outputs.remove(output);
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
							synchronized (outputs) {
								for (Output output : outputs) {
									method.invoke(output, args);
								}
							}
							return null;
						}
					});
		}
		return out;
	}

	Class<Input> getInputType() {
		return input;
	}

	Class<Output> getOutputType() {
		return output;
	}

	public abstract void flush(Output output) throws Exception;
	/*
	 * 
	 */
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

}
