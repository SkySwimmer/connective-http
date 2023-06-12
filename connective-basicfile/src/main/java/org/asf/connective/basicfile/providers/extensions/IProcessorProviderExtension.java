package org.asf.connective.basicfile.providers.extensions;

import org.asf.connective.basicfile.DocumentProcessor;

/**
 * 
 * An interface to allow aliases, file extensions, index pages and restrictions
 * to access the DocumentProcessor instance.
 * 
 * @author Sky Swimmer
 *
 */
public interface IProcessorProviderExtension {

	/**
	 * Provides the document processor instance
	 * 
	 * @param processor DocumentProcessor instance
	 */
	public void provide(DocumentProcessor processor);

}
