package osgi.enroute.logger.simple.provider;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Logger;
import org.slf4j.spi.MDCAdapter;

/*
 * This class is created by the Slf4j API and links into the OSGi logging system.
 */

public class SLF4JHandler implements ILoggerFactory {
	
	@Override
	public Logger getLogger(String name) {
		return new AbstractLogger(LoggerDispatcher.classContext.getCallerBundle(), name);
	}

	public IMarkerFactory getMarkerFactory() {
		// TODO support markers
		throw new UnsupportedOperationException();
	}

	public MDCAdapter getMDCA() {
		// TODO support mdcs
		throw new UnsupportedOperationException();
	}

}
