package org.asf.connective.handlers;

import java.io.IOException;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.handlers.HttpRequestHandler;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * HTTP Request Handler
 * 
 * @author Sky Swimmer
 *
 */
public abstract class HttpRequestHandler extends DynamicHttpRequestHandler {

	@Override
	public boolean handleRequest(String path, String method, RemoteClient client) throws IOException {
		handle(path, method, client);
		return true;
	}

	/**
	 * Instantiates a new handler with the server, request and response
	 * 
	 * @param server   Server to use
	 * @param request  HTTP request
	 * @param response HTTP response
	 * @return New HttpRequestHandler configured for processing
	 */
	@Override
	public HttpRequestHandler instantiate(ConnectiveHttpServer server, HttpRequest request, HttpResponse response) {
		return (HttpRequestHandler) super.instantiate(server, request, response);
	}

	/**
	 * Called to handle the request
	 * 
	 * @param path   Path string
	 * @param method Request method
	 * @param client Remote client
	 * @throws IOException If processing fails
	 */
	public abstract void handle(String path, String method, RemoteClient client) throws IOException;

	/**
	 * Creates a new instance of this HTTP handler
	 */
	@Override
	public abstract HttpRequestHandler createNewInstance();

}
