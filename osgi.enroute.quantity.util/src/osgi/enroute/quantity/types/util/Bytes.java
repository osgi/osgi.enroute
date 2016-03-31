package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.BaseQuantity;
import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;
import osgi.enroute.quantity.base.util.Unit.Dimension;

@UnitInfo(unit="byte", symbol="B", dimension = "Bytes", symbolForDimension = "")
public class Bytes extends BaseQuantity<Bytes> {
	static final double	KILO	= 1024D;
	static final double	MEGA	= KILO * 1024;
	static final double	GIGA	= MEGA * 1024;
	static final double	TERA	= GIGA * 1024;
	static final double	PETA	= TERA * 1024;
	static final double	EXA		= PETA * 1024;
	static final double	ZETTA	= EXA * 1024;
	static final double	YOTTA	= ZETTA * 1024;

	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit				= new Unit(Bytes.class);
	static final Dimension			DIMe1				= Unit.dimension(Bytes.class, 1);
	public static final AbstractConverter<Bytes> DEFAULT_CONVERTER = null; // TODO

	Bytes(double value) {
		super(value);
	}

	@Override
	protected Bytes same(double value) {
		return Bytes.from(value);
	}

	private static Bytes from(double value) {
		return new Bytes(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	@Deprecated
	public Bytes yocto() {
		return mul(  Modifier.YOCTO.multiplier);
	}
	
	@Deprecated
	public Bytes zepto() {
		return mul(  Modifier.ZEPTO.multiplier);
	}
	
	@Deprecated
	public Bytes atto() {
		return mul(  Modifier.ATTO.multiplier);
	}
	
	@Deprecated
	public Bytes femto() {
		return mul(  Modifier.FEMTO.multiplier);
	}
	
	@Deprecated
	public Bytes pico() {
		return mul(  Modifier.PICO.multiplier);
	}
	
	@Deprecated
	public Bytes nano()  {
		return mul(  Modifier.NANO.multiplier);
	}

	@Deprecated
	public Bytes micro() {
		return mul(  Modifier.MICRO.multiplier);
	}

	@Deprecated
	public Bytes milli() {
		return mul(  Modifier.MILLI.multiplier);
	}

	@Deprecated
	public Bytes centi() {
		return mul(  Modifier.CENTI.multiplier);
	}

	@Deprecated
	public Bytes deci() {
		return mul(  Modifier.DECI.multiplier);
	}

	@Deprecated
	public Bytes deca() {
		return mul(  Modifier.DECA.multiplier);
	}

	@Deprecated
	public Bytes hecto() {
		return mul(  Modifier.HECTO.multiplier);
	}
	public Bytes kilo() {
		return mul(  KILO );
	}
	
	public Bytes mega() {
		return mul(  MEGA );
	}
	
	public Bytes giga() {
		return mul(  GIGA);
	}
	
	public Bytes tera() {
		return mul(  TERA);
	}
	
	public Bytes peta() {
		return mul(  PETA);
	}
	
	public Bytes exa() {
		return mul(  EXA);
	}
	
	public Bytes zetta() {
		return mul(  ZETTA);
	}
	
	public Bytes yotta() {
		return mul(  YOTTA);
	}
	

	@Deprecated
	public double toYocto() {
		return value() / Modifier.YOCTO.multiplier;
	}

	@Deprecated
	public double toZepto() {
		return value() / Modifier.ZEPTO.multiplier;
	}

	@Deprecated
	public double toAtto() {
		return value() / Modifier.ATTO.multiplier;
	}
	@Deprecated
	public double toFemto() {
		return value() / Modifier.FEMTO.multiplier;
	}
	@Deprecated
	public double toPico() {
		return value() / Modifier.PICO.multiplier;
	}
	@Deprecated
	public double toNano() {
		return value() / Modifier.NANO.multiplier;
	}
	@Deprecated
	public double toMicro() {
		return value() / Modifier.MICRO.multiplier;
	}

	@Deprecated
	public double toMilli() {
		return value() / Modifier.MILLI.multiplier;
	}

	@Deprecated
	public double toCenti() {
		return value() / Modifier.CENTI.multiplier;
	}

	@Deprecated
	public double toDeci() {
		return value() / Modifier.DECI.multiplier;
	}
	@Deprecated
	public double toDeca() {
		return value() / Modifier.DECA.multiplier;
	}

	@Deprecated
	public double toHecto() {
		return value() / Modifier.HECTO.multiplier;
	}

	public double toKilo() {
		return value() / KILO;
	}

	public double toMega() {
		return value() / MEGA;
	}
	public double toGiga() {
		return value() / GIGA;
	}
	public double toTera() {
		return value() / TERA;
	}
	public double toPeta() {
		return value() / PETA;
	}
	public double toExa() {
		return value() / EXA;
	}
	public double toZetta() {
		return value() / ZETTA;
	}
	public double toYotta() {
		return value() / YOTTA;
	}
	
	public Bandwidth during(Time s) {
		return Bandwidth.fromBytesPerSecond( value / s.value);
	}

	public Bandwidth toBandwidth() {
		return Bandwidth.fromBytesPerSecond( value);
	}

	public static Bytes fromKB(double value) {
		return new Bytes(value*1000);
	}
	
	public double toKB() {
		return value / 1000;
	}
}
