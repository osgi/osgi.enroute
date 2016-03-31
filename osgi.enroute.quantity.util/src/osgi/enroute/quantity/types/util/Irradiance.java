package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit="W/m²", symbol="I", dimension="Intensity", symbolForDimension="")
public class Irradiance extends DerivedQuantity<Irradiance>{
	private static final long serialVersionUID = 1L;
	private static final Unit unit = new Unit(Irradiance.class,Mass.DIMe1, Time.DIMe_3);
	public static final AbstractConverter<Irradiance> DEFAULT_CONVERTER = null; // TODO

	Irradiance(double value) {
		super(value);
	}

	@Override
	protected Irradiance same(double value) {
		return fromWattPerMeter2(value);
	}

	public static Irradiance fromWattPerMeter2(double value) {
		return new Irradiance(value);
	}
	public double toWattPerMeter2() {
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
		return Energy.fromJoule(value / s.value);
	}
		
	public double todB() {
		return Math.log10(value) * 10;
	}
			
	public double todBm() {
		return Math.log10(value) * 10_000;
	}
}
