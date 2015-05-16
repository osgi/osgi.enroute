package osgi.enroute.iot.gpio.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import osgi.enroute.dto.api.DTOs;
import osgi.enroute.iot.admin.dto.ICDTO;
import osgi.enroute.iot.admin.dto.PinDTO;
import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;

/**
 * An IC (from Integrated Circuit) is a component with n inputs and m outputs.
 * The inputs & outputs of an IC are typed by an interface. This interface must
 * have <em>IO</em> methods with simple names that would match names on an IC.
 * This method must have a void return type. It must take a single parameter of
 * any scalar or DTO type. Surprisingly, this method prototype pattern is for
 * the inputs and the outputs. Since inputs and outputs are fundamentally
 * different, this requires some explanation.
 * <p>
 * The purpose of the IC is to allow them to be wired with an external circuit.
 * This circuit can be a GUI that connects the named outputs from one IC to the
 * named inputs of another (or the same) IC. The circuit admin will signal the
 * IC when the signal changes (or when it feels like it).
 * <p>
 * If the IC has a change in signal (or just feels like it) it can call a proxy
 * with the {@link #out()} method. This proxy implements the Output interface
 * and thus has all the methods to set the output.
 * <p>
 * The circuit can connect to the output pins by calling
 * {@link #connect(Output)} and should call disconnect before it goes away. The
 * IC class will call any output on all registered Output objects.
 */
public abstract class ICAdapter<Input, Output> implements IC {
	static Pattern LAST_SEGMENT_P = Pattern.compile(".*[$.]([^.$]+)(?:$\\d+)?");

	private final Class<Output> output;
	private final Class<Input> input;
	private Output out = null;
	private Map<Method, Object> values = new HashMap<Method, Object>();
	private ICDTO icdto = new ICDTO();
	private CircuitBoard board;
	private DTOs dtos;

	private Object[] inputNames;

	static class InputPin extends PinDTO {
		Method method;
	}

	public ICAdapter( String deviceId, DTOs dtos, CircuitBoard board) {
		this();
		setDeviceId(deviceId);
		setDTOs(dtos);
		setCircuitBoard(board);
	}
	/**
	 * Constructor.
	 */
	public ICAdapter() {
		Class<?> rover = this.getClass();
		while (rover.getSuperclass() != ICAdapter.class) {
			rover = rover.getSuperclass();
		}

		ParameterizedType zuper = (ParameterizedType) rover
				.getGenericSuperclass();
		this.input = resolve(zuper.getActualTypeArguments()[0], "Input");
		this.output = resolve(zuper.getActualTypeArguments()[1], "Output");

		if (this.input != null && !this.input.isInstance(this))
			throw new IllegalArgumentException(
					"An IC must implement its Input type parameter. This class "
							+ this.getClass() + " does not implement "
							+ this.input);

		if (this.input == null)
			icdto.inputs = new PinDTO[0];
		else
			//
			// Create the input pins
			//
			icdto.inputs = Stream.of(this.input.getMethods())
					.sorted((a, b) -> a.getName().compareTo(b.getName()))
					.filter((m)-> !(Modifier.isStatic(m.getModifiers()) || m.getParameterTypes().length!=1))
					.map((method) -> {
						InputPin in = new InputPin();
						in.name = method.getName();
						in.type = method.getParameterTypes()[0].getName();
						in.method = method;
						return in;
					}).toArray((n) -> new PinDTO[n]);

		this.inputNames = Stream.of(icdto.inputs).map(pin -> pin.name)
				.toArray(n -> new String[n]);

		if (this.output == null)
			icdto.outputs = new PinDTO[0];
		else {
			//
			// Create the output pins
			//
			icdto.outputs = Stream.of(this.output.getMethods())
					.sorted((a, b) -> a.getName().compareTo(b.getName()))
					.filter((m)-> !(Modifier.isStatic(m.getModifiers()) || m.getParameterTypes().length!=1))
					.map((method) -> {
						PinDTO out = new PinDTO();
						out.name = method.getName();
						out.type = method.getParameterTypes()[0].getName();

						return out;
					}).toArray((n) -> new PinDTO[n]);

		}

		icdto.name = getName();
		icdto.type = getClass().getName();
	}

	@SuppressWarnings("unchecked")
	protected synchronized Output out() {
		if (out == null) {
			out = (Output) Proxy.newProxyInstance(output.getClassLoader(),
					new Class<?>[] { output }, new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method,
								Object[] args) throws Throwable {
							if (board != null && args.length == 1) {
								board.fire(ICAdapter.this, method.getName(),
										args[0]);
							}
							return null;
						}
					});
		}
		return out;
	}

	protected void flush(Output output) {
		for (Entry<Method, Object> e : values.entrySet()) {
			try {
				e.getKey().invoke(output, e.getValue());
			} catch (Exception e1) {
				// TODO
				e1.printStackTrace();
			}
		}
	}

	/*
	 * 
	 */
	@SuppressWarnings("unchecked")
	private <T> Class<T> resolve(Type type, String msg) {
		if (type instanceof Class) {

			Class<T> clazz = (Class<T>) type;
			if (clazz == Void.class || clazz == void.class)
				return null;

			if (!clazz.isInterface())
				throw new IllegalArgumentException("An IC " + msg
						+ " type must be an interface, it is a class "
						+ clazz.getName());

			for (Method m : clazz.getMethods()) {

				if (m.getDeclaringClass() == Object.class)
					continue;

				if (m.getParameterTypes().length != 1) {
					throw new IllegalArgumentException("An IC " + msg
							+ " method must have 1 argument,  " + m
							+ " has a different number of args");
				}
				if (m.getReturnType() == Void.class) {
					throw new IllegalArgumentException("An IC " + msg
							+ " methods must have a void return,  " + m
							+ " has a " + m.getReturnType().getName()
							+ " return");
				}
			}
			return clazz;
		}
		throw new IllegalArgumentException(
				"The "
						+ msg
						+ " type variable is not a class. You must use a concrete interface or class without wildcards, variables, arrays. or parameterized types");
	}

	@Override
	public ICDTO getDTO() {
		return icdto;
	}

	private String getName() {
		String name = getClass().getName();
		Matcher m = LAST_SEGMENT_P.matcher(name);
		if (m.matches())
			return m.group(1);
		else
			return name;
	}

	protected void setCircuitBoard(CircuitBoard board) {
		this.board = board;
	}

	protected void setDeviceId(String id) {
		icdto.deviceId = id;
	}
	
	protected void setDTOs(DTOs dtos) {
		this.dtos = dtos;
	}


	@Override
	public void fire(String pin, Object value) throws Exception {
		InputPin input = getInputPin(pin);
		if (input != null) {
			Type t = input.method.getGenericParameterTypes()[0];
			if ( dtos != null)
				value = dtos.convert(value).to(t);
			else
				System.out.println("No DTOs set for " + this);
			input.method.invoke(this, value);
		}
	}

	public InputPin getInputPin(String name) {
		int index = Arrays.binarySearch(inputNames, name);
		if (index < 0)
			return null;

		return (InputPin) icdto.inputs[index];
	}

}
