package osgi.enroute.logger.api;

/**
 * An enum describing the extended log codes. The ordering is deliberate and
 * must be kept.
 */
public enum Level {
	/**
	 * Trace level – Huge output
	 */
	TRACE, /**
			 * Debug level – Very large output
			 */
	DEBUG, /**
			 * Info – Provide information about processes that go ok
			 */
	INFO, /**
			 * Warning – A failure or unwanted situation that is not blocking
			 */
	WARN, /**
			 * Error – An error situation
			 */
	ERROR, /**
			 * R1 – Auxiliary level 1
			 */
	R1, /**
		 * R2 – Auxiliary level 2
		 */
	R2, /**
		 * R3 – Auxiliary level 3
		 */

	R3, /**
		 * Audit – Legal reasons, should never be suppressed
		 */
	AUDIT
};
