package osgi.enroute.scheduler.simple.adapter;

import java.io.Closeable;
import java.time.temporal.TemporalAdjuster;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import osgi.enroute.scheduler.api.Scheduler;
import aQute.bnd.annotation.metatype.Meta;
import aQute.lib.converter.Converter;

/**
 * 
 */

@Component(name = "osgi.enroute.scheduler.simple")
public class SchedulerSimpleImpl implements Scheduler {
	ScheduledExecutorService es;

	interface Config {
		@Meta.AD(deflt = "10")
		int pool_size();
	}

	Config config;

	@Activate
	void activate(Map<String, Object> map) throws Exception {
		if ( map == null) {
			map  = new HashMap<>();
		}
		config = Converter.cnv(Config.class, map);
		es = Executors.newScheduledThreadPool(config.pool_size());
	}

	@Deactivate
	void deactivate() throws Exception {
		es.shutdownNow();
	}

	@Override
	public <T> Promise<T> delay(Callable<T> callable, long ms) {
		Deferred<T> d = new Deferred<T>();
		
		Runnable c = () -> {
			try {
				T result = callable.call();
				d.resolve(result);
			} catch( Throwable e) {
				d.fail(e);
			}
		};
		
		es.schedule(c, ms, TimeUnit.MILLISECONDS);
		return d.getPromise();
	}

	@Override
	public Closeable schedule(Runnable r, TemporalAdjuster tj) {
		// TODO Auto-generated method stub
		return null;
	}

}
