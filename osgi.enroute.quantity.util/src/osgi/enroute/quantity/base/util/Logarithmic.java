package osgi.enroute.quantity.base.util;

public interface Logarithmic<T extends Quantity<T>> {
	double value();
	T same(double value);
	
	default double todB() {
		return Math.log10(value()) * 10;
	}
	default double todBm() {
		return Math.log10(value()) * 10_000;
	}

	default double todBm(T base) {
		return Math.log10(value()/base.value()) * 10_000;
	}
	default double todB(T base) {
		return Math.log10(value()/base.value()) * 10;
	}

	default double toNeper() {
		return Math.log(value());
	}

	default double toNeper(T base) {
		return Math.log(value()/base.value);
	}
	
}
