package osgi.enroute.scheduler.api;

import org.osgi.dto.DTO;

/**
 * The software utility Cron is a time-based job scheduler in Unix-like computer
 * operating systems. People who set up and maintain software environments use
 * cron to schedule jobs (commands or shell scripts) to run periodically at
 * fixed times, dates, or intervals. It typically automates system maintenance
 * or administration—though its general-purpose nature makes it useful for
 * things like connecting to the Internet and downloading email at regular
 * intervals.[1] The name cron comes from the Greek word for time, χρόνος
 * chronos.
 * <p>
 * The Unix Cron defines a syntax that is used by the Cron service. A user
 * should register a Cron service with the {@link Schedule#CRON} property. The value
 * is according to the {@see http://en.wikipedia.org/wiki/Cron}.
 * <p>
 * 
 * <pre>
 * * * * * * * *
 * | │ │ │ │ │ |
 * | │ │ │ │ │ └ year (optional)
 * | │ │ │ │ └── day of week (0 - 6) (0 to 6 are Sunday to Saturday, or use names; 7 is Sunday, the same as 0)
 * | │ │ │ └──── month (1 - 12)
 * | │ │ └────── day of month (1 - 31)
 * | │ └──────── hour (0 - 23)
 * | └────────── min (0 - 59)
 * └──────────── sec (0-59)
 * </pre>
 * 
 * <pre>
 * Field name   mandatory   Values             Special characters
 * Seconds      Yes         0-59               * / , -
 * Minutes	    Yes	        0-59	           * / , -
 * Hours	    Yes	        0-23	           * / , -
 * Day of month	Yes	        1-31	           * / , - ? L W
 * Month	    Yes	        1-12 or JAN-DEC	   * / , -
 * Day of week	Yes	        0-6 or SUN-SAT	   * / , - ? L #
 * Year	        No	       1970–2099	       * / , -
 * </pre>
 * 
 * <h3>Asterisk ( * )</h3>
 * <p>
 * The asterisk indicates that the cron expression matches for all values of the
 * field. E.g., using an asterisk in the 4th field (month) indicates every
 * month.
 * <h3>Slash ( / )</h3>
 * <p>
 * Slashes describe increments of ranges. For example 3-59/15 in the 1st field
 * (minutes) indicate the third minute of the hour and every 15 minutes
 * thereafter. The form "*\/..." is equivalent to the form "first-last/...",
 * that is, an increment over the largest possible range of the field.
 * <h3>Comma ( , )</h3>
 * <p>
 * Commas are used to separate items of a list. For example, using "MON,WED,FRI"
 * in the 5th field (day of week) means Mondays, Wednesdays and Fridays. Hyphen
 * ( - ) Hyphens define ranges. For example, 2000-2010 indicates every year
 * between 2000 and 2010 AD, inclusive.
 */
public class Schedule extends DTO {
	String	CRON		= "cron";

	
}
