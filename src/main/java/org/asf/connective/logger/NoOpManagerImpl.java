package org.asf.connective.logger;

import java.util.HashMap;

public class NoOpManagerImpl extends ConnectiveLoggerManager {

	private HashMap<String, ConnectiveLogger> loggers = new HashMap<String, ConnectiveLogger>();

	@Override
	public ConnectiveLogger getLogger(String name) {
		synchronized (loggers) {
			if (loggers.containsKey(name))
				return loggers.get(name);
			ConnectiveLogger logger = new NoOpLoggerImpl(this);
			loggers.put(name, logger);
			return logger;
		}
	}

}
