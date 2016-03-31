package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.BaseQuantity;
import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;
/**
 * A gram per kilowatt-hour (g/kW·h) is a metric unit of the power specific fuel consumption.
 */
@UnitInfo(unit = "J",symbol="W",  dimension = "Energy", symbolForDimension = "")
public class EnergyConsumption extends BaseQuantity<EnergyConsumption> {
	private static final long	serialVersionUID	= 1L;
	private static Unit			unit			= new Unit(EnergyConsumption.class, Length.DIMe_1, Time.DIMe2);
	public static final AbstractConverter<EnergyConsumption> DEFAULT_CONVERTER = null; // TODO

	
	EnergyConsumption(double value) {
		super(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public static EnergyConsumption fromKiloGramPerJoule(double value) {
		return new EnergyConsumption(value);
	}
	
	public double toKiloGramPerJoule() {
		return value;
	}
	
	public static EnergyConsumption fromGramPerKWh(double value) {
		return new EnergyConsumption(value*2.777777777778E-10);
	}

	public double toGramPerKWh() {
		return value / 2.777777777778E-10;
	}
	

	@Override
	protected EnergyConsumption same(double value) {
		return fromKiloGramPerJoule(value);
	}


}
