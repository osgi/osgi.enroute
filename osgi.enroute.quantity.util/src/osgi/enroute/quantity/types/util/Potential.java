package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit="V", symbol="U", dimension="Voltage", symbolForDimension="")
public class Potential extends DerivedQuantity<Potential>{
	private static final long serialVersionUID = 1;
	private static final Unit dimension = new Unit(Potential.class, Mass.DIMe1, Length.DIMe2, Current.DIMe_1, Time.DIMe_3 );
	public static final AbstractConverter<Potential> DEFAULT_CONVERTER = null; // TODO

	Potential(double value) {
		super(value);
	}

	@Override
	protected Potential same(double value) {
		return fromVolt(value);
	}

	public static Potential fromVolt(double value) {
		return new Potential(value);
	}
	
	public double toVolt() {
		return value;
	}

	@Override
	public Unit getUnit() {
		return dimension;
	}
	
	public Power power( Current ampere) {
		return Power.fromWatt(value * ampere.value);
	}

	public ReactivePower power( Current ampere, Angle phase) {
		return ReactivePower.fromVA(this,ampere, phase);
	}

	

}
