package osgi.enroute.iot.circuit.toolkit;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import osgi.enroute.dto.api.DTOs;
import osgi.enroute.iot.circuit.toolkit.Wave.WaveConfig;
import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.util.Analog;
import osgi.enroute.iot.gpio.util.ICAdapter;
import osgi.enroute.scheduler.api.Scheduler;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

@Component(designateFactory = WaveConfig.class, provide = IC.class)
public class Wave extends ICAdapter<Void, Analog> {
	enum Shape {
		unity, sin;
	}

	interface WaveConfig {
		String name();

		int steps(int deflt);

		int stepTime(int defltMs);

		Shape shape();

		double gain(double deflt);
	}

	private Scheduler	scheduler;
	private WaveConfig	cfg;
	private int			steps;
	private int			stepTime;
	private int			step;
	private double		delta;
	private double		x	= 0;
	private double		y	= 0;
	private Runnable	f;
	private double		gain;
	private Closeable	schedule;

	@Activate
	void activate(Map<String, Object> map) throws Exception {
		cfg = getDTOs().convert(map).to(WaveConfig.class);
		this.stepTime = Math.max(cfg.stepTime(10), 1);
		this.steps = Math.max(cfg.steps(100), 1);
		this.gain = cfg.gain(1.0D);
		switch (cfg.shape()) {
		case sin:
			this.delta = (2 * Math.PI) / this.steps;
			this.f = this::sin;
			break;

		case unity:
		default:
			this.delta = 1.0D / this.steps;
			this.f = this::unity;
			break;

		}
		this.delta = 1.0D / this.steps;
		schedule = scheduler.schedule(this::tick, this.stepTime, this.stepTime);
	}

	@Deactivate
	void deactivate() throws IOException {
		schedule.close();
	}

	void tick() {
		f.run();
		out().set(y * gain);

		step++;
		if (step > steps) {
			step = 0;
			x = 0;
		} else
			x += delta;
	}

	void sin() {
		y = Math.sin(x);
	}

	void unity() {
		y = x;
	}

	@Reference
	void setScheduler(Scheduler sch) {
		this.scheduler = sch;
	}

	@Reference
	protected void setCircuitBoard(CircuitBoard board) {
		super.setCircuitBoard(board);
	}

	@Reference
	protected void setDTOs(DTOs dtos) {
		super.setDTOs(dtos);
	}

}
