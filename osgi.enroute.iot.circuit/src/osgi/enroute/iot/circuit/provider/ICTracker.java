package osgi.enroute.iot.circuit.provider;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import osgi.enroute.iot.admin.dto.ICDTO;
import osgi.enroute.iot.admin.dto.PinDTO;
import osgi.enroute.iot.gpio.api.IC;

/**
 * This clas tracks the ICs in the registry. It creates the DTO but also
 * maintains the input & output pins. It will register with the IC as an Output
 * proxy.
 * <p>
 * The tracker will have a list of outputs ({@link OutputPin} objects) and
 * inputs ({@link InputPin} objects). The outputs can be subscribed to by an
 * input. This is done during wiring.
 * <p>
 * Though this class is a DTO, it can not be read as a DTO.
 */
public class ICTracker implements InvocationHandler {

	CircuitAdminImpl	admin;
	IC					ic;
	String				inputNames[];
	String				outputNames[];
	OutputPin[]			outputs;
	InputPin[]			inputs;
	Closeable			close;
	Runnable			connect;
	ICDTO				icdto;
	AtomicBoolean		closed	= new AtomicBoolean();

	/*
	 * Represents an input pin. An input pin is notified of changes. An input
	 * pin can subscribe to an output pin.
	 */
	class InputPin {
		PinDTO	pin;
		Object	lastValue;

		public void fire(Object object) throws Exception {
			if (lastValue == object)
				return;

			if (lastValue != null && lastValue.equals(object))
				return;

			pin.value = truthy(object);
			ic.fire(pin.name, object);
			lastValue = object;
		}

		public ICTracker getTracker() {
			return ICTracker.this;
		}
	}

	/*
	 * An output pin can signal input pins if fired.
	 */
	class OutputPin {
		PinDTO					pin;
		final List<InputPin>	subscribers	= new CopyOnWriteArrayList<InputPin>();
		Object					lastValue;

		public void fire(Object object) {
			lastValue = object;
			pin.value = truthy(object);
			for (InputPin in : subscribers) {
				try {
					in.fire(object);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		/*
		 * Subscribe to an output. Note that this method is always called in a
		 * global lock so we don't have to worry about concurrency
		 */
		public void subscribe(InputPin subscriber) {
			subscribers.add(subscriber);
		}

		public boolean unsubscribe(InputPin subscriber) {
			return subscribers.remove(subscriber);
		}

		public ICTracker getTracker() {
			return ICTracker.this;
		}
	}

	boolean truthy(Object object) {
		if (object == null || object == Boolean.FALSE || object.equals(""))
			return false;

		return true;
	}

	/*
	 * Connect a newly discovered IC to
	 */
	<I, O> ICTracker(String id, IC ic, CircuitAdminImpl ca) throws Exception {
		this.ic = ic;
		this.admin = ca;
		this.icdto = admin.dtos.shallowCopy(ic.getDTO());
		this.icdto.deviceId = id;

		this.outputs = Stream.of(this.icdto.outputs)
				.sorted((a, b) -> a.name.compareTo(b.name))//
				.map((out) -> {
					OutputPin pin = new OutputPin();
					pin.pin = out;
					return pin;
				}).toArray(n -> new OutputPin[n]);
		this.inputs = Stream.of(this.icdto.inputs)
				.sorted((a, b) -> a.name.compareTo(b.name))//
				.map((in) -> {
					InputPin pin = new InputPin();
					pin.pin = in;
					return pin;
				}).toArray(n -> new InputPin[n]);
		this.inputNames = Stream.of(this.inputs).map(p -> p.pin.name)
				.toArray(n -> new String[n]);
		this.outputNames = Stream.of(this.outputs).map(p -> p.pin.name)
				.toArray(n -> new String[n]);
	}
	
	String getName() {
		return this.icdto.deviceId;
	}

	public void close() throws IOException {
		if (closed.getAndSet(true)) {
			if (close != null)
				close.close();
		}
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		OutputPin output = getOutputPin(method.getName());
		assert output != null : "These arrays must match up";

		output.fire(args[0]);
		return null;
	}

	public OutputPin getOutputPin(String name) {
		int index = Arrays.binarySearch(outputNames, name);
		if (index < 0)
			return null;

		return (OutputPin) outputs[index];
	}

	public InputPin getInputPin(String name) {
		int index = Arrays.binarySearch(inputNames, name);
		if (index < 0)
			return null;

		return (InputPin) inputs[index];
	}

}
