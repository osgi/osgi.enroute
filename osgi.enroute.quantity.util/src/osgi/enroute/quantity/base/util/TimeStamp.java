package osgi.enroute.quantity.base.util;

import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.UnsupportedTemporalTypeException;

public class TimeStamp implements TemporalAccessor {
	public final long ms;
	
	
	public TimeStamp() {
		ms = System.currentTimeMillis();
	}
	
	public TimeStamp(long ms) {
		this.ms = ms;
	}
	
	public TimeStamp(String ISO) {
		TemporalAccessor parse = DateTimeFormatter.ISO_INSTANT.parse(ISO);
		ms = Instant.from(parse).toEpochMilli();
	}
	
	public String toString() {
		return DateTimeFormatter.ISO_INSTANT.format(this);
	}
	
	@Override
	public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField) {
            return field == INSTANT_SECONDS || field == MILLI_OF_SECOND;
        }
        return field != null && field.isSupportedBy(this);
	}

	@Override
	public long getLong(TemporalField field) {
	    if (field instanceof ChronoField) {
	        switch ((ChronoField) field) {
	            case MILLI_OF_SECOND: return ms;
	            case INSTANT_SECONDS: return ms / 1000;
	            default:
	    	        throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
	        }
	    }
	    return field.getFrom(this);
	}


}
