package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

@UnitInfo(unit="var", symbol="Q", dimension = "Reactive Power", symbolForDimension = "")
public class ReactivePower extends DerivedQuantity<ReactivePower>{
	private static final long serialVersionUID = 1L;
	
	Unit		unit = new Unit(ReactivePower.class, Mass.DIMe1, Length.DIMe2, Time.DIMe_3);
	public static final AbstractConverter<ReactivePower> DEFAULT_CONVERTER = null; // TODO

	ReactivePower(double value) {
		super(value);
	}

	public static ReactivePower fromVA( double value) {
		return new ReactivePower(value);
	}
	public static ReactivePower fromVar( double value) {
		return new ReactivePower(value);
	}
	
	public double toVar( ) {
		return value;
	}
	
	@Override
	protected ReactivePower same(double value) {
		return fromVar(value);
	}
	
	public static ReactivePower fromVA( Potential uRms, Current iRms, Angle phi) {
		return fromVar( uRms.value * iRms.value * phi.sin());
	}
	
	public double toVA() {
		return value;
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

}
