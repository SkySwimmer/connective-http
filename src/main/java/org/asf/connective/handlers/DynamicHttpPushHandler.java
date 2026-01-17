package org.asf.connective.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * HTTP Upload Handler with fallthrough
 * 
 * @author Sky Swimmer
 * @since Connective 1.0.0.A17
 *
 */
public abstract class DynamicHttpPushHandler extends DynamicHttpRequestHandler {

	@Override
	public String[] methods() {
		if (supportsNonPush())
			return new String[] { "GET", "PUT", "POST" };
		else
			return new String[] { "PUT", "POST" };
	}

	/**
	 * Called to verify the request processor against the HTTP resource prior to
	 * running it, returning false will fall through to the following request
	 * handler
	 * 
	 * @param path        Request path
	 * @param method      Request method
	 * @param client      Remote client
	 * @param contentType Body content type
	 * @throws IOException If processing fails
	 * @return True if handled, false otherwise, return false to fall through to the
	 *         next handler
	 * @throws IOException
	 */
	public boolean match(String path, String method, RemoteClient client, String contentType) throws IOException {
		return true;
	}

	@Override
	public boolean match(String path, String method, RemoteClient client) throws IOException {
		return match(path, method, client, null);
	}

	@Override
	public boolean handleRequest(String path, String method, RemoteClient client) throws IOException {
		return handleRequest(path, method, client, null);
	}

	/**
	 * Retrieves the body content stream
	 * 
	 * @return Stream that leads to the request content body
	 */
	protected InputStream getRequestBody() {
		return getRequest().getBodyStream();
	}

	/**
	 * Retrieves the body content as string
	 * 
	 * @return String representing the request body
	 * @throws IOException If reading fails
	 */
	protected String getRequestBodyAsString() throws IOException {
		return getRequest().getRequestBodyAsString();
	}

	/**
	 * Retrieves the body content as string
	 * 
	 * @param encoding Encoding to use
	 * @return String representing the request body
	 * @throws IOException If reading fails
	 */
	protected String getRequestBodyAsString(String encoding) throws IOException {
		return getRequest().getRequestBodyAsString(encoding);
	}

	/**
	 * Retrieves the body content as string
	 * 
	 * @param encoding Encoding to use
	 * @return String representing the request body
	 * @throws IOException If reading fails
	 */
	protected String getRequestBodyAsString(Charset encoding) throws IOException {
		return getRequest().getRequestBodyAsString(encoding);
	}

	/**
	 * Transfers the request body
	 * 
	 * @param output Target stream
	 * @throws IOException If transferring fails
	 */
	protected void transferRequestBody(OutputStream output) throws IOException {
		getRequest().transferRequestBody(output);
	}

	/**
	 * Retrieves the request body length
	 * 
	 * @return Request body length
	 */
	protected long getRequestBodyLength() {
		return getRequest().getBodyLength();
	}

	/**
	 * Instantiates a new handler with the server, request and response
	 * 
	 * @param server   Server to use
	 * @param request  HTTP request
	 * @param response HTTP response
	 * @return New DynamicHttpPushHandler configured for processing
	 */
	public DynamicHttpPushHandler instantiate(ConnectiveHttpServer server, HttpRequest request, HttpResponse response) {
		return (DynamicHttpPushHandler) super.instantiate(server, request, response);
	}

	/**
	 * Checks if the handler support non-push requests, false by default
	 * 
	 * @return True if the handler supports this, false otherwise
	 */
	public boolean supportsNonPush() {
		return false;
	}

	/**
	 * Called to handle the request
	 * 
	 * @param path        Path string
	 * @param method      Request method
	 * @param client      Remote client
	 * @param contentType Body content type
	 * @throws IOException If processing fails
	 * @return True if handled, false otherwise, return false to fall through to the
	 *         next handler
	 */
	public abstract boolean handleRequest(String path, String method, RemoteClient client, String contentType)
			throws IOException;

	/**
	 * Creates a new instance of this HTTP handler
	 */
	public abstract DynamicHttpPushHandler createNewInstance();

}
