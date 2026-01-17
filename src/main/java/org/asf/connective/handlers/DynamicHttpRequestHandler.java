package org.asf.connective.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.handlers.DynamicHttpRequestHandler;
import org.asf.connective.headers.HeaderCollection;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * HTTP Request Handler with fallthrough
 * 
 * @author Sky Swimmer
 * @since Connective 1.0.0.A17
 *
 */
public abstract class DynamicHttpRequestHandler {

	private ConnectiveHttpServer server;
	private HttpResponse response;
	private HttpRequest request;

	/**
	 * Defines the HTTP methods this request handler supports
	 * 
	 * @return Array of HTTP methods supported by handler
	 */
	public String[] methods() {
		return new String[] { "GET" };
	}

	/**
	 * Called to verify the request processor against the HTTP resource prior to
	 * running it, returning false will fall through to the following request
	 * handler
	 * 
	 * @param path   Request path
	 * @param method Request method
	 * @param client Remote client
	 * @throws IOException If processing fails
	 * @return True if handled, false otherwise, return false to fall through to the
	 *         next handler
	 * @throws IOException
	 */
	public boolean match(String path, String method, RemoteClient client) throws IOException {
		return true;
	}

	/**
	 * Instantiates a new handler with the server, request and response
	 * 
	 * @param server   Server to use
	 * @param request  HTTP request
	 * @param response HTTP response
	 * @return New DynamicHttpRequestHandler configured for processing
	 */
	public DynamicHttpRequestHandler instantiate(ConnectiveHttpServer server, HttpRequest request,
			HttpResponse response) {
		DynamicHttpRequestHandler inst = createNewInstance();
		inst.server = server;
		inst.response = response;
		inst.request = request;
		return inst;
	}

	/**
	 * Retrieves the server processing the request
	 * 
	 * @return ConnectiveHTTPServer instance.
	 */
	protected ConnectiveHttpServer getServer() {
		return server;
	}

	/**
	 * Retrieves the request HTTP headers
	 * 
	 * @return Request HTTP headers
	 */
	protected HeaderCollection getHeaders() {
		return getRequest().getHeaders();
	}

	/**
	 * Retrieves a specific request HTTP header
	 * 
	 * @return HTTP header value
	 */
	protected String getHeader(String name) {
		return getRequest().getHeaderValue(name);
	}

	/**
	 * Checks if a specific request HTTP header is present
	 * 
	 * @return True if the header is present, false otherwise
	 */
	protected boolean hasHeader(String name) {
		return getRequest().hasHeader(name);
	}

	/**
	 * Assigns the value of the given HTTP header
	 * 
	 * @param header Header name
	 * @param value  Header value
	 */
	protected void setResponseHeader(String header, String value) {
		getResponse().addHeader(header, value);
	}

	/**
	 * Assigns the value of the given HTTP header
	 * 
	 * @param header Header name
	 * @param value  Header value
	 * @param append True to add to the existing header if present, false to
	 *               overwrite values (clears the header if already present)
	 */
	protected void setResponseHeader(String header, String value, boolean append) {
		getResponse().addHeader(header, value, append);
	}

	/**
	 * Sets the response body
	 * 
	 * @param type Content type
	 * @param body Response body string
	 */
	protected void setResponseContent(String type, String body) {
		getResponse().setContent(type, body);
	}

	/**
	 * Sets the response body (plaintext)
	 * 
	 * @param body Response body string
	 */
	protected void setResponseContent(String body) {
		setResponseContent("text/plain", body);
	}

	/**
	 * Sets the response body (binary)
	 * 
	 * @param type Content type
	 * @param body Response body bytes
	 */
	protected void setResponseContent(String type, byte[] body) {
		getResponse().setContent(type, body);
	}

	/**
	 * Sets the response body (binary)
	 * 
	 * @param body Response body bytes
	 */
	protected void setResponseContent(byte[] body) {
		getResponse().setContent(body);
	}

	/**
	 * Sets the response body (InputStream)
	 * 
	 * @param type Content type
	 * @param body Input stream
	 */
	protected HttpResponse setResponseContent(String type, InputStream body) {
		return getResponse().setContent(type, body);
	}

	/**
	 * Sets the response body (InputStream)
	 * 
	 * @param body Input stream
	 */
	protected HttpResponse setResponseContent(InputStream body) {
		return getResponse().setContent(body);
	}

	/**
	 * Sets the response body (InputStream)
	 * 
	 * @param type   Content type
	 * @param body   Input stream
	 * @param length Content length
	 */
	protected HttpResponse setResponseContent(String type, InputStream body, long length) {
		return getResponse().setContent(type, body, length);
	}

	/**
	 * Sets the response body (InputStream)
	 * 
	 * @param body   Input stream
	 * @param length Content length
	 */
	protected HttpResponse setResponseContent(InputStream body, long length) {
		return getResponse().setContent(body, length);
	}

	/**
	 * Assigns a new response status
	 * 
	 * @param status  New status code
	 * @param message New status message
	 */
	protected HttpResponse setResponseStatus(int status, String message) {
		return response.setResponseStatus(status, message);
	}

	/**
	 * Retrieves the HTTP request object
	 * 
	 * @return HttpRequest instance
	 */
	protected HttpRequest getRequest() {
		return request;
	}

	/**
	 * Retrieves the HTTP response object
	 * 
	 * @return HttpResponse instance
	 */
	protected HttpResponse getResponse() {
		return response;
	}

	/**
	 * Retrieves the HTTP request path
	 * 
	 * @return Request path
	 */
	protected String getRequestPath() {
		return getRequest().getRequestPath();
	}

	/**
	 * Retrieves the HTTP request query
	 * 
	 * @return Request query string
	 */
	protected String getRequestQuery() {
		return getRequest().getRequestQuery();
	}

	/**
	 * Retrieves the map of request query parameters
	 * 
	 * @return Request query parameters
	 */
	protected Map<String, String> getRequestQueryParameters() {
		return getRequest().getRequestQueryParameters();
	}

	/**
	 * Retrieves the unparsed request path string
	 * 
	 * @return HTTP raw request string
	 */
	protected String getRawRequestResource() {
		return getRequest().getRawRequestResource();
	}

	/**
	 * Retrieves the request method
	 * 
	 * @return HTTP request method string
	 */
	protected String getRequestMethod() {
		return getRequest().getRequestMethod();
	}

	/**
	 * Retrieves the path this handler supports
	 * 
	 * @return File path string
	 */
	public abstract String path();

	/**
	 * Checks if this handler supports child paths, false by default
	 * 
	 * @return True if the handler supports this, false otherwise
	 */
	public boolean supportsChildPaths() {
		return false;
	}

	/**
	 * Called to handle the request
	 * 
	 * @param path   Path string
	 * @param method Request method
	 * @param client Remote client
	 * @throws IOException If processing fails
	 * @return True if handled, false otherwise, return false to fall through to the
	 *         next handler
	 */
	public abstract boolean handleRequest(String path, String method, RemoteClient client) throws IOException;

	/**
	 * Creates a new instance of this HTTP handler
	 */
	public abstract DynamicHttpRequestHandler createNewInstance();
}
