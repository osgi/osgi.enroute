package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

/**
 * The ohm (symbol: Ω) is the SI derived unit of electrical resistance, named
 * after German physicist Georg Simon Ohm. Although several empirically derived
 * standard units for expressing electrical resistance were developed in
 * connection with early telegraphy practice, the British Association for the
 * Advancement of Science proposed a unit derived from existing units of mass,
 * length and time and of a convenient size for practical work as early as 1861.
 * The definition of the ohm was revised several times. Today the definition of
 * the ohm is expressed from the quantum Hall effect.
 */
@UnitInfo(unit="Ω/m", symbol = "", dimension = "Electrical resistanceper meter", symbolForDimension = "")
public class ResistancePerMeter extends DerivedQuantity<ResistancePerMeter> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(ResistancePerMeter.class, Mass.DIMe1, Length.DIMe1, Time.DIMe_3,
			Current.DIMe_2);
	public static final AbstractConverter<ResistancePerMeter> DEFAULT_CONVERTER = null; // TODO

	ResistancePerMeter(double value) {
		super(value);
	}

	@Override
	protected ResistancePerMeter same(double value) {
		return fromOhm(value);
	}

	public static ResistancePerMeter fromOhm(double value) {
		return new ResistancePerMeter(value);
	}
	
	public double toOhm() {
		return value;
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public Potential potential(Current amp) {
		return Potential.fromVolt(value * amp.value);
	}

	public Current current(Potential v) {
		return Current.fromAmpere(v.value / value);
	}

	public Conductance conductance() {
		return Conductance.from(1 / value);
	}
	
	public Conductance inverse() {
		return conductance();
	}
}
