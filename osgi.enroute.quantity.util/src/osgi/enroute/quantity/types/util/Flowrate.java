package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

/**
 * In physics and engineering, in particular fluid dynamics and hydrometry, the
 * volumetric flow rate, (also known as volume flow rate, rate of fluid flow or
 * volume velocity) is the volume of fluid which passes per unit time; usually
 * represented by the symbol Q (sometimes V̇). The SI unit is m3/s (cubic metres
 * per second). Another unit used is sccm (standard cubic centimeters per
 * minute).
 * 
 * In US Customary Units and British Imperial Units, volumetric flow rate is
 * often expressed as ft3/s (cubic feet per second) or gallons per minute
 * (either U.S. or imperial definitions).
 * 
 * Volumetric flow rate should not be confused with volumetric flux, as defined
 * by Darcy's law and represented by the symbol q, with units of m3/(m2·s), that
 * is, m·s−1. The integration of a flux over an area gives the volumetric flow
 * rate.
 * 
 * @author aqute
 *
 */
@UnitInfo(unit = "m³/s", symbol = "Flowrate", dimension = "Flowrate", symbolForDimension = "Q")
public class Flowrate extends DerivedQuantity<Flowrate> {

	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(Flowrate.class, Length.DIMe3);
	public static final AbstractConverter<Flowrate> DEFAULT_CONVERTER = null; // TODO

	Flowrate(double value) {
		super(value);
	}

	public static Flowrate fromM3PerSecond(double value) {
		return new Flowrate(value);
	}

	public double toM3PerSecond() {
		return value;
	}
	
	public static Flowrate fromLiterPerHour(double value) {
		return new Flowrate(value * 2.777777777778E-7);
	}

	public double toLiterPerHour() {
		return value / 2.777777777778E-7;
	}
	
	
	@Override
	protected Flowrate same(double value) {
		return fromM3PerSecond(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}


}
