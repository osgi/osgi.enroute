package osgi.enroute.quantity.types.util;

import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Arrays;
import java.util.List;

import osgi.enroute.quantity.base.util.BaseQuantity;
import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;
import osgi.enroute.quantity.base.util.Unit.Dimension;

@UnitInfo(unit = "s", symbol = "t", dimension = "Time", symbolForDimension = "T")
public class Time extends BaseQuantity<Time>implements TemporalAmount {
	private static final long		serialVersionUID	= 1L;
	private static final Unit		unit				= new Unit(Time.class);
	static final Dimension			DIMe1				= Unit.dimension(Time.class, 1);
	static final Dimension			DIMe2				= Unit.dimension(Time.class, 2);
	static final Dimension			DIMe3				= Unit.dimension(Time.class, 3);
	static final Dimension			DIMe4				= Unit.dimension(Time.class, 4);
	static final Dimension			DIMe_1				= Unit.dimension(Time.class, -1);
	public static final Dimension	DIMe_2				= Unit.dimension(Time.class, -2);
	public static final Dimension	DIMe_3				= Unit.dimension(Time.class, -3);
	public static final Dimension	DIMe_4				= Unit.dimension(Time.class, -4);
	public static final Time		SECOND					= new Time(1);
	public static final AbstractConverter<Time> DEFAULT_CONVERTER = null; // TODO

	Time(double value) {
		super(value);
	}

	@Override
	protected Time same(double value) {
		return fromSecond(value);
	}

	public static Time fromSecond(double value) {
		return new Time(value);
	}
	
	public double toSecond() {
		return value;
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public Duration toDuration() {
		if (this.value > Long.MAX_VALUE / 2_000)
			return Duration.of((long) this.value, ChronoUnit.SECONDS);
		if (this.value > Long.MAX_VALUE / 2_000_000)
			return Duration.of((long) this.value * 1000, ChronoUnit.MILLIS);
		if (this.value > Long.MAX_VALUE / 2_000_000_000)
			return Duration.of((long) this.value * 1_000_000, ChronoUnit.MICROS);
		return Duration.of((long) this.value * 1_000_000_000, ChronoUnit.NANOS);
	}

	@Override
	public long get(TemporalUnit unit) {
		if (unit == ChronoUnit.SECONDS)
			return (long) value;

		if (unit == ChronoUnit.NANOS) {
			long nanos = (long) (value * 1_000_000_000) % 1_000_000_000;
			return nanos;
		}

		throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
	}

	@Override
	public List<TemporalUnit> getUnits() {
		return Arrays.asList(ChronoUnit.SECONDS, ChronoUnit.NANOS);
	}

	@Override
	public Temporal addTo(Temporal temporal) {
		long seconds = (long) value;
		temporal = temporal.plus(seconds, SECONDS);

		long nanos = (long) (value * 1_000_000_000) % 1_000_000_000;
		if (nanos != 0)
			temporal = temporal.plus(nanos, NANOS);
		return temporal;
	}

	@Override
	public Temporal subtractFrom(Temporal temporal) {
		long seconds = (long) value;
		temporal = temporal.minus(seconds, SECONDS);

		long nanos = (long) (value * 1_000_000_000) % 1_000_000_000;
		if (nanos != 0)
			temporal = temporal.minus(nanos, NANOS);
		return temporal;
	}

}
