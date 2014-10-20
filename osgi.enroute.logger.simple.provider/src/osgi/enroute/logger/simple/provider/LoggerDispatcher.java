package osgi.enroute.logger.simple.provider;

import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * This is the core dispatcher. We need to use statics because slf4j can be
 * initialized long before we're started. So this dispatcher maintains a queue
 * to hold any entries until a face becomes available.
 */

class LoggerDispatcher {
	
	static Bundle	thisbundle	= FrameworkUtil.getBundle(SLF4JHandler.class);

	//
	// Helper to find out about the caller context
	// this allows us to find the caller bundle
	// This is not safe, but should be good enough for
	// logging
	//
	static class ClassContext extends SecurityManager {

		public Bundle getCallerBundle() {
			for (Class< ? > cc : getClassContext()) {
				Bundle b = FrameworkUtil.getBundle(cc);
				if (b != null && !b.equals(thisbundle))
					return b;
			}
			return thisbundle;
		}
	};
	static ClassContext	classContext	= new ClassContext();

	//
	// Allows synchronized evaluation of the current loggers
	//
	
	interface Eval {
		void eval(AbstractLogger msf);
	}

	void evaluate(Eval r) {
		synchronized (loggers) {
			for (AbstractLogger msf : loggers.keySet()) {
				r.eval(msf);
			}
		}
	}

	//
	// YUck Yck Yk ... This is the first shared static in a LONG time :-(
	//
	static LoggerDispatcher						dispatcher	= new LoggerDispatcher();			// YUCK!!!!!
	
	
	final BlockingQueue<Entry>					queue		= new ArrayBlockingQueue<>(100);
	final WeakHashMap<AbstractLogger,Object>	loggers		= new WeakHashMap<>();
	volatile LoggerAdminImpl					admin;

	/*
	 * Register a logger
	 */
	void register(AbstractLogger mf) {
		synchronized (loggers) {
			loggers.put(mf, null);
		}
	}

	/*
	 * Unregister a logger
	 */
	void unregister(AbstractLogger mf) {
		synchronized (loggers) {
			loggers.remove(mf);
		}
	}

	
}
