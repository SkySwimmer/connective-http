package org.asf.connective.logger;

/**
 * 
 * ConnectiveHTTP Logger Manager
 * 
 * @author Sky Swimmer
 * 
 */
public abstract class ConnectiveLoggerManager {

	/**
	 * Logger implementation
	 */
	protected static ConnectiveLoggerManager implementation = new NoOpManagerImpl();

	/**
	 * Retrieves the logger manager instance
	 * 
	 * @return ConnectiveLoggerManager instance
	 */
	public static ConnectiveLoggerManager getInstance() {
		return implementation;
	}

	/**
	 * Retrieves a logger by name
	 * 
	 * @param name Logger name
	 * @return ConnectiveLogger instance
	 */
	public abstract ConnectiveLogger getLogger(String name);

}
