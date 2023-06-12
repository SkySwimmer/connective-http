package org.asf.connective.basicfile.providers.extensions;

import org.asf.connective.RemoteClient;

/**
 * 
 * An interface to allow aliases, file extensions, index pages and restrictions
 * to access the client instance making requests.
 * 
 * @author Sky Swimmer
 *
 */
public interface IRemoteClientProviderExtension {

	/**
	 * Provides the RemoteClient instance making the HTTP request
	 * 
	 * @param client RemoteClient instance
	 */
	public void provide(RemoteClient client);

}
