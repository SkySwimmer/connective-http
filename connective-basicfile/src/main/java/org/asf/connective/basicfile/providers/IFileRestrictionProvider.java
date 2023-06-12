package org.asf.connective.basicfile.providers;

import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * File restriction provider - to secure file storage where needed
 * 
 * @author Sky Swimmer
 *
 */
public interface IFileRestrictionProvider {

	/**
	 * Creates a new instance of the restriction provider
	 * 
	 * @return New IFileRestrictionProvider instance
	 */
	public IFileRestrictionProvider createInstance();

	/**
	 * Checks if the given request should be restricted by this restriction provider
	 * 
	 * @param request   Request instance
	 * @param inputPath Request path (potentially aliased by another alias)
	 * @return True if the restriction is valid and should be processed for this
	 *         request, false otherwise
	 */
	public boolean match(HttpRequest request, String inputPath);

	/**
	 * Checks file access, false disconnects the client with a 403, true allows the
	 * request.<br />
	 * <br />
	 * <b>WARNING:</b> Windows paths are case-insensitive, make sure you keep that
	 * in mind when writing a restriction provider!
	 * 
	 * @param file     Path to the file or directory to check
	 * @param request  Request instance
	 * @param response Response instance
	 * @return True if access is granted, false otherwise
	 */
	public boolean checkRestriction(String file, HttpRequest request, HttpResponse response);

	/**
	 * Retrieves the response code used when the restriction blocks the request
	 * 
	 * @param request Request instance
	 * @return Response status code
	 */
	public default int getResponseCode(HttpRequest request) {
		return 403;
	}

	/**
	 * Retrieves the response message used when the restriction blocks the request
	 * 
	 * @param request Request instance
	 * @return Response status message
	 */
	public default String getResponseMessage(HttpRequest request) {
		return "Forbidden";
	}

	/**
	 * Rewrites the response run when the restriction blocks the request
	 * 
	 * @param request  Request instance
	 * @param response Response instance
	 */
	public default void rewriteResponse(HttpRequest request, HttpResponse response) {
	}

}
