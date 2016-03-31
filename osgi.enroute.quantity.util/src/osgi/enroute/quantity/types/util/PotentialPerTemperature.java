package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit = "V", symbol = "U", dimension = "Voltage", symbolForDimension = "")
public class PotentialPerTemperature extends DerivedQuantity<PotentialPerTemperature> {
	private static final long	serialVersionUID	= 1;
	private static final Unit	dimension			= new Unit(PotentialPerTemperature.class, Mass.DIMe1,
			Length.DIMe2, Current.DIMe_1, Time.DIMe_3, Temperature.DIMe_1);
	public static final AbstractConverter<PotentialPerTemperature> DEFAULT_CONVERTER = null; // TODO

	PotentialPerTemperature(double value) {
		super(value);
	}

	@Override
	protected PotentialPerTemperature same(double value) {
		return fromPotentialPerTemperature(value);
	}

	public static PotentialPerTemperature fromPotentialPerTemperature(double value) {
		return new PotentialPerTemperature(value);
	}

	public double toPotentialPerTemperature() {
		return value;
	}

	public double toVolt(Temperature t) {
		return value * t.value;
	}

	@Override
	public Unit getUnit() {
		return dimension;
	}

}
