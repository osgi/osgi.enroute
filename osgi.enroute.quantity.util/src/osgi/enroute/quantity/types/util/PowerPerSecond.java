package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Logarithmic;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

/**
 * Terms such as watts per hour are often misused when watts would be
 * correct.[21] Watts per hour properly refers to the change of power per hour.
 * Watts per hour (W/h) might be useful to characterize the ramp-up behavior of
 * power plants. For example, a power plant that reaches a power output of 1 MW
 * from 0 MW in 15 minutes has a ramp-up rate of 4 MW/h. Hydroelectric power
 * plants have a very high ramp-up rate, which makes them particularly useful in
 * peak load and emergency situations.
 * 
 */
@UnitInfo(unit = "W/s", symbol = "", dimension = "PowerPerSecond", symbolForDimension = "")
public class PowerPerSecond extends DerivedQuantity<PowerPerSecond>implements Logarithmic<PowerPerSecond> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(PowerPerSecond.class, Mass.DIMe1, Length.DIMe2,
			Time.DIMe_4);
	public static final AbstractConverter<PowerPerSecond> DEFAULT_CONVERTER = null; // TODO

	PowerPerSecond(double value) {
		super(value);
	}

	@Override
	public PowerPerSecond same(double value) {
		return fromWatt(value);
	}

	public static PowerPerSecond fromWatt(double value) {
		return new PowerPerSecond(value);
	}

	public double toWatt() {
		return value;
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public Current toCurrent(Potential volt) {
		return Current.fromAmpere(value / volt.value);
	}

	public Potential toVolt(Current amp) {
		return Potential.fromVolt(value / amp.value);
	}

	public Energy toJoule(Time s) {
		return Energy.fromJoule(value * s.value);
	}

	public Irradiance intensity(Area area) {
		return Irradiance.fromWattPerMeter2(value / area.value);
	}
}
