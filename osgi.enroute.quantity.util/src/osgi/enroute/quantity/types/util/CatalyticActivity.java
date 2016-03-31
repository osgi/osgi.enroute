package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

/**
 * 
 * The katal (symbol: kat) is the SI unit of catalytic activity.[1] It is a
 * derived SI unit for quantifying the catalytic activity of enzymes (measuring
 * the enzymatic activity level in enzyme catalysis) and other catalysts. Its
 * use is recommended by the General Conference on Weights and Measures and
 * other international organizations. It replaces the non-SI enzyme unit. Enzyme
 * units are, however, still more commonly used than the katal in practice at
 * present, especially in biochemistry.
 * 
 * The katal is not used to express the rate of a reaction; that is expressed in
 * units of concentration per second (or moles per liter per second). Rather, it
 * is used to express catalytic activity which is a property of the catalyst.
 * The katal is invariant of the measurement procedure, but the numerical
 * quantity value is not and depends on the experimental conditions. Therefore,
 * in order to define the quantity of a catalyst, the rate of conversion of a
 * defined chemical reaction is specified as mols reacted per second. One katal
 * of trypsin, for example, is that amount of trypsin which breaks a mole of
 * peptide bonds per second under specified conditions.
 */
@UnitInfo(unit="kat",symbol="", dimension="Catalytic activity", symbolForDimension="")
public class CatalyticActivity extends DerivedQuantity<CatalyticActivity> {
	private static final long	serialVersionUID	= 1L;
	private static final Unit	unit			= new Unit(CatalyticActivity.class,	Substance.DIMe1, Time.DIMe_1);
	public static final AbstractConverter<CatalyticActivity> DEFAULT_CONVERTER = null; // TODO

	public CatalyticActivity(double value) {
		super(value);
	}

	@Override
	protected CatalyticActivity same(double value) {
		return from(value);
	}

	public static CatalyticActivity from(double value) {
		return new CatalyticActivity(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	
	public Substance duration(Time second) {
		return Substance.from(value * second.value);
	}
	
}
