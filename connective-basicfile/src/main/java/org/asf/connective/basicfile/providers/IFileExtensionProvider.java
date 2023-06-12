package org.asf.connective.basicfile.providers;

import java.io.IOException;
import java.io.InputStream;

import org.asf.connective.basicfile.util.FileContext;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * File extension provider - rewrites file output based on the file's extension
 * 
 * @author Sky Swimmer
 *
 */
public interface IFileExtensionProvider {

	/**
	 * Creates a new instance of the extension provider
	 * 
	 * @return New IFileExtensionProvider instance
	 */
	public IFileExtensionProvider createInstance();

	/**
	 * Retrieves the file extension used by this provider
	 * 
	 * @return File extension (eg. '.php')
	 */
	public String fileExtension();

	/**
	 * Rewrites a file request
	 * 
	 * @param path       Request path
	 * @param fileSource Source file input stream
	 * @param input      Input response
	 * @param request    Input request
	 * @return FileContext containing the rewritten file request
	 * @throws IOException If processing fails
	 */
	public FileContext rewrite(String path, InputStream fileSource, HttpResponse input, HttpRequest request)
			throws IOException;

}
