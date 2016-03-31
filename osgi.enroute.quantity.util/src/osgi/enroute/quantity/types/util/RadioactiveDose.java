package osgi.enroute.quantity.types.util;

import osgi.enroute.quantity.base.util.AbstractConverter;
import osgi.enroute.quantity.base.util.DerivedQuantity;
import osgi.enroute.quantity.base.util.Unit;
import osgi.enroute.quantity.base.util.UnitInfo;

/**
 * The gray (symbol: Gy) is a derived unit of ionizing radiation dose in the
 * International System of Units (SI). It is defined as the absorption of one
 * joule of radiation energy by one kilogram of matter. It is used as a measure
 * of absorbed dose, specific energy (imparted), and kerma.
 * 
 */

@UnitInfo(unit = "Gy", symbol="D", dimension = "Radioactive dose", symbolForDimension = "R")
public class RadioactiveDose extends DerivedQuantity<RadioactiveDose> {
	private static final long	serialVersionUID	= 1;
	private static final Unit	unit				= new Unit(RadioactiveDose.class,		//
			//
			Length.DIMe2,														//
			Time.DIMe_2														//
														);
	public static final AbstractConverter<RadioactiveDose> DEFAULT_CONVERTER = null; // TODO

	public RadioactiveDose(double value) {
		super(value);
	}

	@Override
	protected RadioactiveDose same(double value) {
		return from(value);
	}

	public static RadioactiveDose from(double value) {
		return new RadioactiveDose(value);
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

}
