package org.asf.connective.logger;

import org.asf.connective.RemoteClient;

/**
 * 
 * ConnectiveHTTP Log Message
 * 
 * @author Sky Swimmer
 * 
 */
public class ConnectiveLogMessage {

	private String type;
	private String message;
	private Exception exception;
	private RemoteClient client;

	public ConnectiveLogMessage(String type, String message, Exception exception, RemoteClient client) {
		this.type = type;
		this.message = message;
		this.exception = exception;
		this.client = client;
	}

	/**
	 * Retrieves the log message type string
	 * 
	 * @return Log message type
	 */
	public String getMessageType() {
		return type;
	}

	/**
	 * Retrieves the log message body
	 * 
	 * @return Log message body
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Checks if the log message is accompanied by an exceptio
	 * 
	 * @return True if an exception is present, false otherwise
	 */
	public boolean hasException() {
		return exception != null;
	}

	/**
	 * Retrieves the exception instance
	 * 
	 * @return Exception instance or null
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * Checks if a client object is present
	 * 
	 * @return True if present, false otherwise
	 */
	public boolean hasClient() {
		return client != null;
	}

	/**
	 * Retrieves the client instance
	 * 
	 * @return Client instance or null if not present
	 */
	public RemoteClient getClient() {
		return client;
	}

	/**
	 * Resolves the log-friendly client address string
	 * 
	 * @return Log-formatted client address string or null
	 */
	public String resolvePrettyAddressString() {
		if (client == null)
			return null;

		// Compute address
		String addr = client.getRemoteAddress();
		if (client.isProxied()) {
			// Update
			if (!client.passedThroughAuthoritiveProxy())
				addr = client.getRemoteProxiedClientAddress();

			// Add proxy chain
			for (String proxy : client.getProxyChain()) {
				addr += " -> " + proxy;
			}

			// Add warning if needed
			if (!client.passedThroughAuthoritiveProxy())
				addr += " (ALERT! detected non-authoritive proxy, using address " + client.getRemoteAddress() + ")";
		}
		return addr;
	}

}
