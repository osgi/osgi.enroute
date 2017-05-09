package osgi.enroute.iot.pi.provider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import aQute.lib.converter.Converter;
import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.util.ICAdapter;
import osgi.enroute.iot.gpio.util.Wave;

@ObjectClassDefinition
@interface LircConfig {
	int device();
}

/**
 * It is hard to find any information about the LIRC interface. From
 * http://www.nishioka.com/train/ we learned that the LIRC is basically a
 * sequence of int's where each int specifies the width of a pulse. The first
 * one is on, the second one off, third on. The total number must be odd, the
 * LIRC driver switches off at the end automatically.
 *
 * According to our sources, the option softcarrier=1 must be chosen but not
 * found anywhere this is defined. On a Raspberry, make sure you configure the
 * Pi. On modern OS's this requires that you use the device tree.
 *
 * <pre>
 * 	/boot/config.txt:
 * 	dtoverlay=dtoverlay=lirc-rpi,softcarrier=0
 * </pre>
 *
 * By default the IR out signal is on GPIO17 (GPIO00 for Pi4J numbering).
 * However, with the dtoverlay you can override it.
 *
 */

@Designate(ocd=LircConfig.class, factory=true)
@Component(name = "osgi.enroute.iot.lirc", service = IC.class)
public class LIRCImpl extends ICAdapter<Wave, Void> implements Wave {
	private ByteOrder endianness = ByteOrder.nativeOrder();
	private File file;

	@Activate
	void activate(Map<String, Object> map) throws Exception {
		LircConfig config = Converter.cnv(LircConfig.class, map);
		String path = "/dev/lirc" + config.device();
		this.file = new File(path);
		if (!file.exists())
			throw new ConfigurationException(
					path
							+ " does not exist. LIRC requires device tree + dtoverlay=lirc-rpi,softcarrier=0 in /boot/config.txt");

		endianness = ByteOrder.LITTLE_ENDIAN;
	}

	@Deactivate
	void deactivate() throws IOException {
	}

	@Override
	public void send(int[] times) throws Exception {
		ByteBuffer pulses = ByteBuffer.allocate(times.length * 4);
		pulses.order(endianness);

		int length = times.length;

		// lirc_rpi doesn't want to see trailing space (requires count to be
		// odd)

		if ((times.length & 1) == 0)
			length--;

		for (int i = 0; i < length; i++) {
			pulses.putInt(times[i] & 0xFF_FF_FF);
		}

		byte[] array = pulses.array();

		try (FileOutputStream fd = new FileOutputStream(file)) {
			fd.write(array, 0, length * 4);
		}
	}

	@Reference
	protected void setCircuitBoard(CircuitBoard board) {
		super.setCircuitBoard(board);
	}
}
