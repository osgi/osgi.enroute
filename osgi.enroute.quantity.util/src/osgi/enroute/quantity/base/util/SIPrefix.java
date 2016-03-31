package osgi.enroute.quantity.base.util;

public interface SIPrefix<T> {
	
	enum Modifier {
		YOCTO("y", "yocto", -24, 1e-24d), //
		ZEPTO("z", "zepto", -21, 1e-21d), //
		ATTO("a", "atto", -18, 1e-18d), //
		FEMTO("f", "femto", -15, 1e-15d), //
		PICO("p", "pico", -12, 1e-12d), //
		NANO("n", "nano", -9, 1e-9d), //
		MICRO("µ", "micro", -6, 1e-6d), //
		MILLI("m", "milli", -3, 0.001d), //
		CENTI("c", "centi", -2, 0.01d), //
		DECI("d", "deci", -1, 0.1d), //
		UNIT("", "", 0, 1d), //
		DECA("da", "deca", 1, 10d), //
		HECTO("h", "hecto", 2, 100d), //
		KILO("k", "kilo", 3, 1000d), //
		MEGA("M", "mega", 6, 1e6d), //
		GIGA("G", "giga", 9, 1e9d), //
		TERA("T", "tera", 12, 1e12d), //
		PETA("P", "peta", 15, 1e15d), //
		EXA("E", "exa", 18, 1e18d), //
		ZETTA("Z", "zetta", 21, 1e21d), //
		YOTTA("Y", "yotta", 24, 1e24d);

		public final String name;
		public final String symbol;
		public final int exponent;
		public final double multiplier;

		private Modifier(String symbol, String name, int exponent, double multiplier) {
			this.symbol = symbol;
			this.name = name;
			this.exponent = exponent;
			this.multiplier = multiplier;
		}
		
		
	}

	T mul( double value);
	 double value();
	
	default T yocto() {
		return mul(  Modifier.YOCTO.multiplier);
	}
	
	default T zepto() {
		return mul(  Modifier.ZEPTO.multiplier);
	}
	
	default T atto() {
		return mul(  Modifier.ATTO.multiplier);
	}
	
	default T femto() {
		return mul(  Modifier.FEMTO.multiplier);
	}
	
	default T pico() {
		return mul(  Modifier.PICO.multiplier);
	}
	
	default T nano()  {
		return mul(  Modifier.NANO.multiplier);
	}

	default T micro() {
		return mul(  Modifier.MICRO.multiplier);
	}

	default T milli() {
		return mul(  Modifier.MILLI.multiplier);
	}

	default T centi() {
		return mul(  Modifier.CENTI.multiplier);
	}

	default T deci() {
		return mul(  Modifier.DECI.multiplier);
	}

	default T deca() {
		return mul(  Modifier.DECA.multiplier);
	}

	default T hecto() {
		return mul(  Modifier.HECTO.multiplier);
	}
	default T kilo() {
		return mul(  Modifier.KILO.multiplier);
	}
	
	default T mega() {
		return mul(  Modifier.MEGA.multiplier);
	}
	
	default T giga() {
		return mul(  Modifier.GIGA.multiplier);
	}
	
	default T tera() {
		return mul(  Modifier.TERA.multiplier);
	}
	
	default T peta() {
		return mul(  Modifier.PETA.multiplier);
	}
	
	default T exa() {
		return mul(  Modifier.EXA.multiplier);
	}
	
	default T zetta() {
		return mul(  Modifier.ZETTA.multiplier);
	}
	
	default T yotta() {
		return mul(  Modifier.YOTTA.multiplier);
	}
	

	default double toYocto() {
		return value() / Modifier.YOCTO.multiplier;
	}

	default double toZepto() {
		return value() / Modifier.ZEPTO.multiplier;
	}

	default double toAtto() {
		return value() / Modifier.ATTO.multiplier;
	}
	default double toFemto() {
		return value() / Modifier.FEMTO.multiplier;
	}
	default double toPico() {
		return value() / Modifier.PICO.multiplier;
	}
	default double toNano() {
		return value() / Modifier.NANO.multiplier;
	}
	default double toMicro() {
		return value() / Modifier.MICRO.multiplier;
	}

	default double toMilli() {
		return value() / Modifier.MILLI.multiplier;
	}

	default double toCenti() {
		return value() / Modifier.CENTI.multiplier;
	}

	default double toDeci() {
		return value() / Modifier.DECI.multiplier;
	}
	default double toDeca() {
		return value() / Modifier.DECA.multiplier;
	}

	default double toHecto() {
		return value() / Modifier.HECTO.multiplier;
	}

	default double toKilo() {
		return value() / Modifier.KILO.multiplier;
	}

	default double toMega() {
		return value() / Modifier.MEGA.multiplier;
	}
	default double toGiga() {
		return value() / Modifier.GIGA.multiplier;
	}
	default double toTera() {
		return value() / Modifier.TERA.multiplier;
	}
	default double toPeta() {
		return value() / Modifier.PETA.multiplier;
	}
	default double toExa() {
		return value() / Modifier.EXA.multiplier;
	}
	default double toZetta() {
		return value() / Modifier.ZETTA.multiplier;
	}
	default double toYotta() {
		return value() / Modifier.YOTTA.multiplier;
	}
	
	
}
