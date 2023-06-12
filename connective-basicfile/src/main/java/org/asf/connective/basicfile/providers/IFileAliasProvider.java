package org.asf.connective.basicfile.providers;

import org.asf.connective.objects.HttpRequest;

/**
 * 
 * File Alias Provider Interface
 * 
 * @author Sky Swimmer
 *
 */
public interface IFileAliasProvider {

	/**
	 * Creates a new instance of the alias provider
	 * 
	 * @return New IFileAliasProvider instance
	 */
	public IFileAliasProvider createInstance();

	/**
	 * Checks if the given request should be aliased by this alias provider
	 * 
	 * @param request   Request instance
	 * @param inputPath Request path (potentially aliased by another alias)
	 * @return True if the request alias is valid, false otherwise
	 */
	public boolean match(HttpRequest request, String inputPath);

	/**
	 * Returns the aliased path
	 * 
	 * @param request   Request instance
	 * @param inputPath Previous request path (potentially aliased by another alias)
	 * @return New path string
	 */
	public String applyAlias(HttpRequest request, String inputPath);

}
