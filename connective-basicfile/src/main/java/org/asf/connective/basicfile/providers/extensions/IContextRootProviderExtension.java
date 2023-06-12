package org.asf.connective.basicfile.providers.extensions;

/**
 * 
 * An interface to allow extensions to access the context root (virtual root)
 * 
 * @author Sky swimmer
 *
 */
public interface IContextRootProviderExtension {

	/**
	 * Provides the virtual root associated with the current context
	 * 
	 * @param virtualRoot Virtual root path
	 */
	public void provideVirtualRoot(String virtualRoot);

}
