package osgi.enroute.iot.pi.command;

import java.io.IOException;
import java.text.ParseException;
import java.util.Formatter;

import org.apache.felix.service.command.Parameter;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import osgi.enroute.debug.api.Debug;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.system.NetworkInfo;
import com.pi4j.system.SystemInfo;

/**
 * This is the implementation. It registers the Pi interface and calls it
 * through a Gogo command.
 * 
 * TODO handle differences in Pi types
 */
@Component(service = PiCommand.class, property = { Debug.COMMAND_SCOPE + "=pi",Debug.COMMAND_FUNCTION + "=pi", 
		Debug.COMMAND_FUNCTION + "=pins", Debug.COMMAND_FUNCTION + "=high",
		Debug.COMMAND_FUNCTION + "=blink", Debug.COMMAND_FUNCTION + "=low",
		Debug.COMMAND_FUNCTION + "=create", Debug.COMMAND_FUNCTION + "=info",
		Debug.COMMAND_FUNCTION + "=test", Debug.COMMAND_FUNCTION + "=reset" }, name = "osgi.enroute.iot.pi.command")
public class PiCommand {

	GpioController gpio;

	static Pin[] pins = { RaspiPin.GPIO_00, RaspiPin.GPIO_01, RaspiPin.GPIO_02,
			RaspiPin.GPIO_03, RaspiPin.GPIO_04, RaspiPin.GPIO_05,
			RaspiPin.GPIO_06, RaspiPin.GPIO_07, RaspiPin.GPIO_08,
			RaspiPin.GPIO_09, RaspiPin.GPIO_10, RaspiPin.GPIO_11,
			RaspiPin.GPIO_12, RaspiPin.GPIO_13, RaspiPin.GPIO_14,
			RaspiPin.GPIO_15, RaspiPin.GPIO_16, RaspiPin.GPIO_17,
			RaspiPin.GPIO_18, RaspiPin.GPIO_19, RaspiPin.GPIO_20,
			RaspiPin.GPIO_21, RaspiPin.GPIO_22, RaspiPin.GPIO_23,
			RaspiPin.GPIO_24, RaspiPin.GPIO_25, RaspiPin.GPIO_26,
			RaspiPin.GPIO_27, RaspiPin.GPIO_28, RaspiPin.GPIO_29 };

	public String pi() {
		return "pi:* commands. These directly manipulate the GpioController.\n"
				+ "create <name> <pin>           – create a digital pin\n"
				+ "test <name>                   – set a pin low/high 20 times\n"
				+ "pins                          – show the pins\n"
				+ "blink <name> <time>           – use the Pi4J blink function\n"
				+ "high <name>                   – set a pin high\n" 
				+ "low <name>                    – set a pin low\n" 
				+ "info                          – show all the info of the board\n" 
				+ "reset                         – reset the controller\n"
				+ "\n" //
				+ "Pin numbers follow Pi4J GPIO numbers, see http://pi4j.com/pins/model-2b-rev1.html";
	}

	public void create(String name, @Parameter(absentValue = "false", presentValue="true", names = { "--in", "-i" }) boolean in, int n) {
		Pin defpin = pins[n];
		GpioPinDigitalOutput out = gpio.provisionDigitalOutputPin(defpin, name);
		System.out.println(out + " " + out.getMode());
	}

	public void test(String name) throws InterruptedException {
		GpioPinDigitalOutput pin = (GpioPinDigitalOutput) get(name);

		pin.export(PinMode.DIGITAL_OUTPUT);

		for (int i = 0; i < 200; i++) {
			pin.low();
			Thread.sleep(100);
			pin.high();
			Thread.sleep(100);
		}
	}

	private GpioPin get(String name) {
		for (GpioPin pin : gpio.getProvisionedPins()) {
			if (pin.getName().equals(name))
				return pin;
		}
		throw new IllegalArgumentException("No such pin " + name);
	}

	public void pins() {
		for (GpioPin pin : gpio.getProvisionedPins()) {
			System.out.println(pin + " " + gpio.getMode(pin));
		}
	}

	public void high(String name) {
		((GpioPinDigitalOutput) get(name)).high();
	}

	public void blink(String name, int time) {
		((GpioPinDigitalOutput) get(name)).blink(time);
	}

	public void low(String name) {
		((GpioPinDigitalOutput) get(name)).low();
	}

	public void reset() {
	}

	public Formatter info() throws IOException, InterruptedException,
			ParseException {
		// display a few of the available system information properties
		Formatter f = new Formatter();

		f.format("----------------------------------------------------\n"
				+ "HARDWARE INFO\n"
				+ "----------------------------------------------------\n");

		f.format("Serial Number     :  %s\n", SystemInfo.getSerial());
		f.format("CPU Revision      :  %s\n", SystemInfo.getCpuRevision());
		f.format("CPU Architecture  :  %s\n", SystemInfo.getCpuArchitecture());
		f.format("CPU Part          :  %s\n", SystemInfo.getCpuPart());
		f.format("CPU Temperature   :  %s\n", SystemInfo.getCpuTemperature());
		f.format("CPU Core Voltage  :  %s\n", SystemInfo.getCpuVoltage());
		f.format("CPU Model Name    :  %s\n", SystemInfo.getModelName());
		f.format("Processor         :  %s\n", SystemInfo.getProcessor());
		f.format("Hardware Revision :  %s\n", SystemInfo.getRevision());
		f.format("Is Hard Float ABI :  %s\n", SystemInfo.isHardFloatAbi());
		f.format("Board Type        :  %s\n", SystemInfo.getBoardType().name());

		f.format("----------------------------------------------------\n"
				+ "MEMORY INFO\n"
				+ "----------------------------------------------------\n");
		f.format("Total Memory      :  %s\n", SystemInfo.getMemoryTotal());
		f.format("Used Memory       :  %s\n", SystemInfo.getMemoryUsed());
		f.format("Free Memory       :  %s\n", SystemInfo.getMemoryFree());
		f.format("Shared Memory     :  %s\n", SystemInfo.getMemoryShared());
		f.format("Memory Buffers    :  %s\n", SystemInfo.getMemoryBuffers());
		f.format("Cached Memory     :  %s\n", SystemInfo.getMemoryCached());
		f.format("SDRAM_C Voltage   :  %s\n",
				SystemInfo.getMemoryVoltageSDRam_C());
		f.format("SDRAM_I Voltage   :  %s\n",
				SystemInfo.getMemoryVoltageSDRam_I());
		f.format("SDRAM_P Voltage   :  %s\n",
				SystemInfo.getMemoryVoltageSDRam_P());

		f.format("----------------------------------------------------\n"
				+ "OPERATING SYSTEM INFO\n"
				+ "----------------------------------------------------\n");
		f.format("OS Name           :  %s\n", SystemInfo.getOsName());
		f.format("OS Version        :  %s\n", SystemInfo.getOsVersion());
		f.format("OS Architecture   :  %s\n", SystemInfo.getOsArch());
		f.format("OS Firmware Build :  %s\n", SystemInfo.getOsFirmwareBuild());
		f.format("OS Firmware Date  :  %s\n", SystemInfo.getOsFirmwareDate());

		f.format("----------------------------------------------------\n"
				+ "JAVA ENVIRONMENT INFO\n"
				+ "----------------------------------------------------\n");
		f.format("Java Vendor       :  %s\n", SystemInfo.getJavaVendor());
		f.format("Java Vendor URL   :  %s\n", SystemInfo.getJavaVendorUrl());
		f.format("Java Version      :  %s\n", SystemInfo.getJavaVersion());
		f.format("Java VM           :  %s\n",
				SystemInfo.getJavaVirtualMachine());
		f.format("Java Runtime      :  %s\n", SystemInfo.getJavaRuntime());

		f.format("----------------------------------------------------\n"
				+ "NETWORK INFO\n"
				+ "----------------------------------------------------\n");
		// display some of the network information
		f.format("Hostname          :  %s\n", NetworkInfo.getHostname());
		for (String ipAddress : NetworkInfo.getIPAddresses())
			f.format("IP Addresses      :  %s\n", ipAddress);
		for (String fqdn : NetworkInfo.getFQDNs())
			f.format("FQDN              :  %s\n", fqdn);
		for (String nameserver : NetworkInfo.getNameservers())
			f.format("Nameserver        :  %s\n", nameserver);

		f.format("----------------------------------------------------\n"
				+ "CODEC INFO\n"
				+ "----------------------------------------------------\n");
		f.format("H264 Codec Enabled:  %s\n", SystemInfo.getCodecH264Enabled());
		f.format("MPG2 Codec Enabled:  %s\n", SystemInfo.getCodecMPG2Enabled());
		f.format("WVC1 Codec Enabled:  %s\n", SystemInfo.getCodecWVC1Enabled());

		f.format("----------------------------------------------------\n"
				+ "CLOCK INFO\n"
				+ "----------------------------------------------------\n");
		f.format("ARM Frequency     :  %s\n", SystemInfo.getClockFrequencyArm());
		f.format("CORE Frequency    :  %s\n",
				SystemInfo.getClockFrequencyCore());
		f.format("H264 Frequency    :  %s\n",
				SystemInfo.getClockFrequencyH264());
		f.format("ISP Frequency     :  %s\n", SystemInfo.getClockFrequencyISP());
		f.format("V3D Frequency     :  %s\n", SystemInfo.getClockFrequencyV3D());
		f.format("UART Frequency    :  %s\n",
				SystemInfo.getClockFrequencyUART());
		f.format("PWM Frequency     :  %s\n", SystemInfo.getClockFrequencyPWM());
		f.format("EMMC Frequency    :  %s\n",
				SystemInfo.getClockFrequencyEMMC());
		f.format("Pixel Frequency   :  %s\n",
				SystemInfo.getClockFrequencyPixel());
		f.format("VEC Frequency     :  %s\n", SystemInfo.getClockFrequencyVEC());
		f.format("HDMI Frequency    :  %s\n",
				SystemInfo.getClockFrequencyHDMI());
		f.format("DPI Frequency     :  %s\n", SystemInfo.getClockFrequencyDPI());

		f.format("\n");
		f.format("\n");
		return f;
	}

	@Reference
	void setGpioController(GpioController controller) {
		this.gpio = controller;
	}
}
