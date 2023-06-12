package org.asf.connective.basicfile.providers;

import org.asf.connective.RemoteClient;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * Virtual file provider - a system for virtual HTTP files that uses the Basic
 * File Context system
 * 
 * @author Sky Swimmer
 *
 */
public interface IVirtualFileProvider {

	/**
	 * Creates a new instance of the virtual file provider
	 * 
	 * @return New IVirtualFileProvider instance
	 */
	public IVirtualFileProvider createInstance();

	/**
	 * Checks if the given request should be handled by this virtual file
	 * 
	 * @param request Request instance
	 * @param path    Request path
	 * @return True if the request is valid for this virtual file, false otherwise
	 */
	public boolean match(HttpRequest request, String path);

	/**
	 * Handles requests made for this virtual file
	 * 
	 * @param method          Request method
	 * @param request         Request instance
	 * @param response        Response instance
	 * @param path            Request path (potentially aliased by another alias)
	 * @param uploadMediaType Upload media type (null if not a upload request)
	 * @param client          Client making the request
	 */
	public void process(String method, HttpRequest request, HttpResponse response, String path, String uploadMediaType,
			RemoteClient client);

	/**
	 * Defines if the virtual file supports push calls (eg. PUT, POST, DELETE)
	 * 
	 * @return True if the virtual file supports modification operations, false if
	 *         it should cause 403 if modification is attempted
	 */
	public default boolean supportsPush() {
		return false;
	}
}
