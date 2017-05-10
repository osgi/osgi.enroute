package org.slf4j.impl;

import org.slf4j.spi.LoggerFactoryBinder;

import osgi.enroute.logger.simple.provider.SLF4JHandler;

/**
 * This class bridges between SLF4j by implementing a getSingleton() method on
 * the class with this name. Unbelievable ugly ... Makes one thankful to have a
 * Service Registry ...
 */
public class StaticLoggerBinder implements LoggerFactoryBinder {
	public static String			REQUESTED_API_VERSION	= "1.7";					// !final

	static final StaticLoggerBinder	SINGLETON				= new StaticLoggerBinder();

	public static final StaticLoggerBinder getSingleton() {
		return SINGLETON;
	}

	final SLF4JHandler	factory	= new SLF4JHandler();

	public SLF4JHandler getLoggerFactory() {
		return factory;
	}

	public String getLoggerFactoryClassStr() {
		return factory.getClass().getName();
	}
}