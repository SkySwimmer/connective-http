package org.asf.connective.handlers;

import java.io.IOException;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * HTTP Upload Handler
 * 
 * @author Sky Swimmer
 *
 */
public abstract class HttpPushHandler extends DynamicHttpPushHandler {

	@Override
	public boolean handleRequest(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		handle(path, method, client, contentType);
		return true;
	}

	/**
	 * Instantiates a new handler with the server, request and response
	 * 
	 * @param server   Server to use
	 * @param request  HTTP request
	 * @param response HTTP response
	 * @return New HttpPushHandler configured for processing
	 */
	@Override
	public HttpPushHandler instantiate(ConnectiveHttpServer server, HttpRequest request, HttpResponse response) {
		return (HttpPushHandler) super.instantiate(server, request, response);
	}

	/**
	 * Called to handle the request
	 * 
	 * @param path        Path string
	 * @param method      Request method
	 * @param client      Remote client
	 * @param contentType Body content type
	 * @throws IOException If processing fails
	 */
	public abstract void handle(String path, String method, RemoteClient client, String contentType) throws IOException;

	/**
	 * Creates a new instance of this HTTP handler
	 */
	@Override
	public abstract HttpPushHandler createNewInstance();

}
