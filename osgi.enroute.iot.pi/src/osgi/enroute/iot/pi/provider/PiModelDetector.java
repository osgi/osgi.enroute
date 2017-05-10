package osgi.enroute.iot.pi.provider;

import java.io.IOException;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.system.SystemInfo;
import com.pi4j.wiringpi.GpioInterruptEvent;

/**
 * This component detects the type of Pi and registers an appropriate service.
 * This service contains a number of service properties that identify the
 * hardware. The purpose of this component is to get an appropriate component
 * started.
 */
@Component(immediate = true)
public class PiModelDetector {
	GpioController gpio;
	ServiceRegistration<GpioController> registration;

	@Activate
	void activate(BundleContext context) throws IOException,
			InterruptedException {

		// For some reason we need to preload this class
		new GpioInterruptEvent("SS", 0, false);

		gpio = GpioFactory.getInstance();
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		props.put("serial.number", SystemInfo.getSerial());
		props.put("cpu.revision", SystemInfo.getCpuRevision());
		props.put("cpu.model", SystemInfo.getModelName());
		props.put("hardware.revision", SystemInfo.getRevision());
		props.put("board.type", SystemInfo.getBoardType().name());
		registration = context.registerService(GpioController.class, gpio,
				props);
	}

	@Deactivate
	void deactivate() {
		registration.unregister();
		gpio.shutdown();
	}
}
