package org.asf.connective.basicfile.providers.extensions;

import org.asf.connective.basicfile.FileProviderContext;

/**
 * 
 * An interface to allow aliases, file extensions, index pages and restrictions
 * to access the executing context.
 * 
 * @author Sky Swimmer
 *
 */
public interface IContextProviderExtension {

	/**
	 * Provides the context instance
	 * 
	 * @param context FileProviderContext instance
	 */
	public void provide(FileProviderContext context);

}
