package org.asf.connective.logger;

/**
 * 
 * ConnectiveHTTP Logger Interface
 * 
 * @author Sky Swimmer
 * 
 */
public interface ConnectiveLogger {

	/**
	 * Retrieves the logger manager used by this logger
	 * 
	 * @return ConnectiveLoggerManager instance
	 */
	public ConnectiveLoggerManager getManager();

	/**
	 * Called to log an error message
	 * 
	 * @param message Message to log
	 */
	public void error(ConnectiveLogMessage message);

	/**
	 * Called to log a warning message
	 * 
	 * @param message Message to log
	 */
	public void warn(ConnectiveLogMessage message);

	/**
	 * Called to log an info message
	 * 
	 * @param message Message to log
	 */
	public void info(ConnectiveLogMessage message);

	/**
	 * Called to log a debug message
	 * 
	 * @param message Message to log
	 */
	public void debug(ConnectiveLogMessage message);

}
