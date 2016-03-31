package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Logarithmic;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit="W", symbol="P", dimension="Power", symbolForDimension="")
public class Power extends DerivedQuantity<Power> implements Logarithmic<Power>{
	private static final long serialVersionUID = 1L;
	private static final Unit unit = new Unit(Power.class,Mass.DIMe1, Length.DIMe2, Time.DIMe_3);
	public static final AbstractConverter<Power> DEFAULT_CONVERTER = null; // TODO

	Power(double value) {
		super(value);
	}

	@Override
	public Power same(double value) {
		return fromWatt(value);
	}

	public static Power fromWatt(double value) {
		return new Power(value);
	}

	public double toWatt() {
		return value;
	}
	
	@Override
	public Unit getUnit() {
		return unit;
	}

	public Current toCurrent( Potential volt) {
		return Current.fromAmpere(value / volt.value);
	}

	public Potential toVolt( Current amp) {
		return Potential.fromVolt(value / amp.value);
	}
	
	public Energy toJoule( Time s) {
		return Energy.fromJoule(value * s.value);
	}
		
	public Irradiance intensity( Area area) {
		return Irradiance.fromWattPerMeter2(value / area.value);
	}
}
