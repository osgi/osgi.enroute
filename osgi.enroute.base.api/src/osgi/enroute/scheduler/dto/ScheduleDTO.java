package osgi.enroute.scheduler.dto;

public class ScheduleDTO {
	public long				bundleId;
	public long				serviceId;
	public ScheduleType		type	= ScheduleType.OTHER;
	public String			cronExpression;
	public long				periods;
	public ScheduleState	state	= ScheduleState.UNKNOWN;
	public long				scheduleTime;
	public long				nextFireTime;
	public long				accumulatedRunningTime;
	public String			lastException;
}
