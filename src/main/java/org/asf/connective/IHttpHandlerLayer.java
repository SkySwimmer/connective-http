package org.asf.connective;

import java.io.IOException;

import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * HTTP Handling layer, used to add additional handling logic executed before
 * content sources receive the request
 * 
 * @author Sky Swimmer
 * @since Connective 1.0.0.A17
 * 
 */
public interface IHttpHandlerLayer {

	/**
	 * Called to handle the request, returning false will cancel handling
	 * 
	 * @param path     Request path
	 * @param request  Request object
	 * @param response Response output object
	 * @param client   Client making the request
	 * @param server   Server instance
	 * @return True to run the request, false to cancel
	 * @throws IOException If handling fails
	 */
	public abstract boolean handle(String path, HttpRequest request, HttpResponse response, RemoteClient client,
			ConnectiveHttpServer server) throws IOException;

}
