package org.asf.connective.basicfile.providers.extensions;

import org.asf.connective.ConnectiveHttpServer;

/**
 * 
 * An interface to allow extensions to access the server instances
 * 
 * @author Sky swimmer
 *
 */
public interface IServerProviderExtension {

	/**
	 * Provides the server instance that is processing the request
	 * 
	 * @param server Server instances
	 */
	public void provide(ConnectiveHttpServer server);

}
