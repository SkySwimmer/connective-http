package org.asf.connective.logger;

public class NoOpLoggerImpl implements ConnectiveLogger {

	private ConnectiveLoggerManager manager;

	public NoOpLoggerImpl(ConnectiveLoggerManager manager) {
		this.manager = manager;
	}

	@Override
	public void error(ConnectiveLogMessage message) {
	}

	@Override
	public void warn(ConnectiveLogMessage message) {
	}

	@Override
	public void info(ConnectiveLogMessage message) {
	}

	@Override
	public void debug(ConnectiveLogMessage message) {
	}

	@Override
	public ConnectiveLoggerManager getManager() {
		return manager;
	}

}
