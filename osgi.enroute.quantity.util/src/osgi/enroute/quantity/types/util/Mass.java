package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.BaseQuantity;
import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;
import osgi.enroute.quantity.base.util.Unit.Dimension;

@UnitInfo(unit = "kg", symbol = "m", dimension = "Mass", symbolForDimension = "M", description = "The amount of matter in an object")
public class Mass extends BaseQuantity<Mass> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(Mass.class);
	public static final Dimension		DIMe1				= Unit.dimension(Mass.class, 1);
	public static Dimension			DIMe_1				= Unit.dimension(Mass.class, -1);
	public static final AbstractConverter<Mass> DEFAULT_CONVERTER = null; // TODO

	public Mass(double value) {
		super(value);
	}

	@Override
	protected Mass same(double value) {
		return from(value);
	}

	public static Mass from(double value) {
		return new Mass(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	public static Mass fromTonne(double tonne) {
		return from(tonne * 1000d);
	}

	public double toTonne() {
		return value / 1000d;
	}

	public static Mass fromGram(double gram) {
		return from(gram / 1000d);
	}

	public double toGram() {
		return value * 1000d;
	}

	public static Mass fromMilligram(double mg) {
		return from(mg / 1_000_000d);
	}

	public double toMilligram() {
		return value * 1_000_000d;
	}

	public static Mass fromMicrogram(double µg) {
		return from(µg / 1_000_000_000d);
	}

	public double toMicrogram() {
		return value * 1_000_000_000d;
	}

	public static Mass fromImperialTon(double imperialTon) {
		return from(imperialTon * 1016.05d);
	}

	public double toImperialTon() {
		return value / 1016.05d;
	}

	public static Mass fromUSTon(double usTon) {
		return from(usTon * 907.185);
	}

	public double toUSTon() {
		return value / 907.185d;
	}

	public static Mass fromStone(double stone) {
		return from(stone * 6.35029d);
	}

	public double toStone() {
		return value / 6.35029d;
	}

	public static Mass fromPound(double pound) {
		return from(pound * 0.453592d);
	}

	public double toPound() {
		return value / 0.453592d;
	}

	public static Mass fromOunce(double ounce) {
		return from(ounce * 0.0283495d);
	}

	public double toOunce() {
		return value / 0.0283495d;
	}

	public Mass yocto() {
		return mul(  Modifier.YOCTO.multiplier / 1000d);
	}
	
	public Mass zepto() {
		return mul(  Modifier.ZEPTO.multiplier / 1000d);
	}
	
	public Mass atto() {
		return mul(  Modifier.ATTO.multiplier / 1000d);
	}
	
	public Mass femto() {
		return mul(  Modifier.FEMTO.multiplier / 1000d);
	}
	
	public Mass pico() {
		return mul(  Modifier.PICO.multiplier / 1000d);
	}
	
	public Mass nano()  {
		return mul(  Modifier.NANO.multiplier / 1000d);
	}

	public Mass micro() {
		return mul(  Modifier.MICRO.multiplier / 1000d);
	}

	public Mass milli() {
		return mul(  Modifier.MILLI.multiplier / 1000d);
	}

	public Mass centi() {
		return mul(  Modifier.CENTI.multiplier / 1000d);
	}

	public Mass deci() {
		return mul(  Modifier.DECI.multiplier / 1000d);
	}

	public Mass deca() {
		return mul(  Modifier.DECA.multiplier / 1000d);
	}

	public Mass hecto() {
		return mul(  Modifier.HECTO.multiplier / 1000d);
	}
	public Mass kilo() {
		return this;
	}
	
	public Mass mega() {
		return mul(  Modifier.MEGA.multiplier / 1000);
	}
	
	public Mass giga() {
		return mul(  Modifier.GIGA.multiplier /  1000d);
	}
	
	public Mass tera() {
		return mul(  Modifier.TERA.multiplier / 1000d);
	}
	
	public Mass peta() {
		return mul(  Modifier.PETA.multiplier / 1000d);
	}
	
	public Mass exa() {
		return mul(  Modifier.EXA.multiplier / 1000d);
	}
	
	public Mass zetta() {
		return mul(  Modifier.ZETTA.multiplier / 1000d);
	}
	
	public Mass yotta() {
		return mul(  Modifier.YOTTA.multiplier / 1000d);
	}
	

	public double toYocto() {
		return value() / Modifier.YOCTO.multiplier * 1000d;
	}

	public double toZepto() {
		return value() / Modifier.ZEPTO.multiplier * 1000d;
	}

	public double toAtto() {
		return value() / Modifier.ATTO.multiplier * 1000d;
	}
	public double toFemto() {
		return value() / Modifier.FEMTO.multiplier * 1000d;
	}
	public double toPico() {
		return value() / Modifier.PICO.multiplier * 1000d;
	}
	public double toNano() {
		return value() / Modifier.NANO.multiplier * 1000d;
	}
	public double toMicro() {
		return value() / Modifier.MICRO.multiplier * 1000d;
	}

	public double toMilli() {
		return value() / Modifier.MILLI.multiplier * 1000d;
	}

	public double toCenti() {
		return value() / Modifier.CENTI.multiplier * 1000d;
	}

	public double toDeci() {
		return value() / Modifier.DECI.multiplier * 1000d;
	}
	
	public double toDeca() {
		return value() / Modifier.DECA.multiplier * 1000d;
	}

	public double toHecto() {
		return value() / Modifier.HECTO.multiplier * 1000d;
	}

	public double toKilo() {
		return value() / Modifier.KILO.multiplier * 1000d;
	}

	public double toMega() {
		return value() / Modifier.MEGA.multiplier * 1000d;
	}
	public double toGiga() {
		return value() / Modifier.GIGA.multiplier * 1000d;
	}
	public double toTera() {
		return value() / Modifier.TERA.multiplier * 1000d;
	}
	public double toPeta() {
		return value() / Modifier.PETA.multiplier * 1000d;
	}
	public double toExa() {
		return value() / Modifier.EXA.multiplier * 1000d;
	}
	public double toZetta() {
		return value() / Modifier.ZETTA.multiplier * 1000d;
	}
	public double toYotta() {
		return value() / Modifier.YOTTA.multiplier * 1000d;
	}
}
