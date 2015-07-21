package osgi.enroute.iot.lego.adapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import osgi.enroute.debug.api.Debug;
import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.util.Analog;
import osgi.enroute.iot.gpio.util.ICAdapter;

/**
 * http://www.nishioka.com/train/
 */
@Component(service = IC.class, property = {
		Debug.COMMAND_SCOPE + "=lego", Debug.COMMAND_FUNCTION + "=fwd",
		Debug.COMMAND_FUNCTION + "=brake", Debug.COMMAND_FUNCTION + "=out" })
public class Command extends ICAdapter<Void, Analog>{
	final int PULSE_WIDTH = 13;
	int toggle = 0;
	int channel = 0;
	int output = 0;

	int send_low(ByteBuffer pulses) {
		int i;
		for (i = 0; i < 11; i++) {
			pulses.putInt(PULSE_WIDTH);
		}

		// Extend final pause to finish symbol
		pulses.putInt(PULSE_WIDTH + 10 * PULSE_WIDTH * 2);

		return 12;
	}

	int send_high(ByteBuffer pulses) {
		int i;
		for (i = 0; i < 11; i++) {
			pulses.putInt(PULSE_WIDTH);
		}

		// Extend final pause to finish symbol
		pulses.putInt(PULSE_WIDTH + 21 * PULSE_WIDTH * 2);

		return 12;
	}

	int send_start(ByteBuffer pulses) {
		int i;
		for (i = 0; i < 11; i++) {
			pulses.putInt(PULSE_WIDTH);
		}

		// Extend final pause to finish symbol
		pulses.putInt(PULSE_WIDTH + 39 * PULSE_WIDTH * 2);

		return 12;
	}

	int send_stop(ByteBuffer pulses) {
		return send_start(pulses);
	}

	// Calculate and insert LRC check bits
	// Send 16 bit word with start and stop
	int send_word(ByteBuffer pulses, int data) {
		int i;
		int count;

		// Calculate check LRC bits
		int check = 0xf ^ (data >> 12) ^ (data >> 8) ^ (data >> 4);

		// Insert into data nibble 4
		data &= ~0xf;
		data |= check & 0xf;

		System.out.printf("send %04x\n", data);

		count = send_start(pulses);
		for (i = 0; i < 16; i++) {
			if ((data & 0x8000) != 0)
				count += send_high(pulses);
			else
				count += send_low(pulses);

			data <<= 1;
		}
		count += send_stop(pulses);

		return count;
	}

	int insert_channel_output(int data, int toggle, int channel, int output) {
		// Insert toggle
		data |= (toggle & 0x1) << 15;

		// Insert channel
		data |= (channel & 0x3) << 12;

		// Insert output number
		data |= (output & 0x1) << 8;

		return (data);
	}

	int send_brake(ByteBuffer pulses, int toggle, int channel, int output) {
		return send_word(pulses,
				insert_channel_output(0x0480, toggle, channel, output));
	}

	int send_increment_pwm(ByteBuffer pulses, int toggle, int channel,
			int output) {
		return send_word(pulses,
				insert_channel_output(0x0640, toggle, channel, output));
	}

	int send_decrement_pwm(ByteBuffer pulses, int toggle, int channel,
			int output) {
		return send_word(pulses,
				insert_channel_output(0x0650, toggle, channel, output));
	}

	int send_forward_pwm(ByteBuffer pulses, int toggle, int channel,
			int output, int step) {
		int command = 0x0400 | ((step & 0x7) << 4);
		return send_word(pulses,
				insert_channel_output(command, toggle, channel, output));
	}

	int send_reverse_pwm(ByteBuffer pulses, int toggle, int channel,
			int output, int step) {
		int command = 0x0480 | ((8 - (step & 0x7)) << 4);
		return send_word(pulses,
				insert_channel_output(command, toggle, channel, output));
	}

	public void fwd() throws IOException, InterruptedException {
		toggle++;

		int speed = 1;
		int i;
		int tm = 16;

		ByteBuffer pulses = ByteBuffer.allocate(1000 * 4);
		pulses.order(ByteOrder.LITTLE_ENDIAN);

		int count = 0;

		count = send_forward_pwm(pulses, toggle, channel, output, speed);

		// lirc_rpi doesn't want to see trailing space (requires count to be
		// odd)
		count--;

		// Open lirc_rpi devices directly
		FileOutputStream fd = new FileOutputStream(new File("/dev/lirc0"));

		byte[] array = pulses.array();
		System.out.println("L=" + array.length + " /4 = " + array.length / 4
				+ " count = " + count);

		// When a button is held down and the protocol needs update to prevent
		// timeout the message is send continuously with a time interval as
		// between message 4 and 5. First after all buttons are released and
		// this is transmitted the transmitter will shut down.
		//
		// If tm is the maximum message length (16ms) and Ch is the channel
		// number, then the delay before transmitting the first message is then
		// the delay before transmitting the first message is (4 – Ch)*tm
		//

		// The time from start to start for the next 2 messages is: The time
		// from start to start for the following messages is:

		// Send command 5 times

		long now = System.currentTimeMillis();

		for (i = 0; i < 5; i++) {
			long delay;

			switch (i) {
			default:
			case 0:
				delay = 0;
				break;

			// (4 – Ch)*tm
			case 1:
				delay = 5 * tm;
				break;

			case 2:
				delay = (4 - channel) * tm;
				break;

			// The time from start to start for the following messages is:
			// (6 + 2*Ch)*tm
			case 3:
			case 4:
				delay = (6 + 2 * channel) * tm;
			}

			long now2 = System.currentTimeMillis();
			delay = now + delay - now2;
			if (delay > 0) {
				Thread.sleep(delay);
				now = now2;
			}

			fd.write(array, 0, count * 4);
			fd.flush();
		}
		fd.close();

	}
	
	public void out( double value ) {
		out().set(value);
	}

	@Reference
	protected void setCircuitBoard(CircuitBoard cb) {
		super.setCircuitBoard(cb);
	}
}
