package osgi.enroute.iot.toolkit;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import osgi.enroute.dto.api.DTOs;
import osgi.enroute.iot.gpio.api.CircuitBoard;
import osgi.enroute.iot.gpio.api.IC;
import osgi.enroute.iot.gpio.util.Analog;
import osgi.enroute.iot.gpio.util.ICAdapter;
import osgi.enroute.iot.toolkit.Wave.WaveConfig;
import osgi.enroute.scheduler.api.Scheduler;

@Designate(ocd=WaveConfig.class, factory=true)
@Component(service = IC.class, name="osgi.enroute.iot.toolkit.wave")
public class Wave extends ICAdapter<Void, Analog> {
	enum Shape {
		unity, sin;
	}

	@ObjectClassDefinition
	@interface WaveConfig {
		String name();

		int steps() default 100;

		int stepTime() default 10;

		Shape shape();

		double gain() default 1.0D;
	}

	private Scheduler	scheduler;
	private WaveConfig	cfg;
	private int			steps;
	private int			stepTime;
	private int			step;
	private double		delta;
	private double		x	= 0;
	private double		y	= 0;
	private Runnable	function;
	private double		gain;
	private Closeable	schedule;

	@Activate
	void activate(Map<String, Object> map) throws Exception {
		cfg = getDTOs().convert(map).to(WaveConfig.class);
		this.stepTime = Math.max(cfg.stepTime(), 1);
		this.steps = Math.max(cfg.steps(), 1);
		this.gain = cfg.gain();
		switch (cfg.shape()) {
		case sin:
			this.delta = (2 * Math.PI) / this.steps;
			this.function = this::sin;
			break;

		case unity:
		default:
			this.delta = 1.0D / this.steps;
			this.function = this::unity;
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
		function.run();
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
