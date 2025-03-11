package org.asf.connective.lambda;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.headers.HeaderCollection;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

public class LambdaPushContext {

	private RemoteClient client;
	private HttpRequest request;
	private HttpResponse response;
	private ConnectiveHttpServer server;
	private String contentType;

	public LambdaPushContext(RemoteClient client, HttpRequest request, HttpResponse response,
			ConnectiveHttpServer server, String contentType) {
		this.client = client;
		this.request = request;
		this.response = response;
		this.server = server;
		this.contentType = contentType;
	}

	/**
	 * Retrieves the request content type
	 * 
	 * @return Content type string
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Retrieves the body content stream
	 * 
	 * @return Stream that leads to the request content body
	 */
	public InputStream getRequestBody() {
		return getRequest().getBodyStream();
	}

	/**
	 * Retrieves the body content as string
	 * 
	 * @return String representing the request body
	 * @throws IOException If reading fails
	 */
	public String getRequestBodyAsString() throws IOException {
		return getRequest().getRequestBodyAsString();
	}

	/**
	 * Retrieves the body content as string
	 * 
	 * @param encoding Encoding to use
	 * @return String representing the request body
	 * @throws IOException If reading fails
	 */
	public String getRequestBodyAsString(String encoding) throws IOException {
		return getRequest().getRequestBodyAsString(encoding);
	}

	/**
	 * Retrieves the body content as string
	 * 
	 * @param encoding Encoding to use
	 * @return String representing the request body
	 * @throws IOException If reading fails
	 */
	public String getRequestBodyAsString(Charset encoding) throws IOException {
		return getRequest().getRequestBodyAsString(encoding);
	}

	/**
	 * Transfers the request body
	 * 
	 * @param output Target stream
	 * @throws IOException If transferring fails
	 */
	public void transferRequestBody(OutputStream output) throws IOException {
		getRequest().transferRequestBody(output);
	}

	/**
	 * Retrieves the request body length
	 * 
	 * @return Request body length
	 */
	public long getRequestBodyLength() {
		return getRequest().getBodyLength();
	}

	/**
	 * Retrieves the client making the request
	 * 
	 * @return RemoteClient instance.
	 */
	public RemoteClient getClient() {
		return client;
	}

	/**
	 * Retrieves the server processing the request
	 * 
	 * @return ConnectiveHTTPServer instance.
	 */
	public ConnectiveHttpServer getServer() {
		return server;
	}

	/**
	 * Retrieves the request HTTP headers
	 * 
	 * @return Request HTTP headers
	 */
	public HeaderCollection getHeaders() {
		return getRequest().getHeaders();
	}

	/**
	 * Retrieves a specific request HTTP header
	 * 
	 * @return HTTP header value
	 */
	public String getHeader(String name) {
		return getRequest().getHeaderValue(name);
	}

	/**
	 * Checks if a specific request HTTP header is present
	 * 
	 * @return True if the header is present, false otherwise
	 */
	public boolean hasHeader(String name) {
		return getRequest().hasHeader(name);
	}

	/**
	 * Assigns the value of the given HTTP header
	 * 
	 * @param header Header name
	 * @param value  Header value
	 */
	public void setResponseHeader(String header, String value) {
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
	public void setResponseHeader(String header, String value, boolean append) {
		getResponse().addHeader(header, value, append);
	}

	/**
	 * Sets the response body
	 * 
	 * @param type Content type
	 * @param body Response body string
	 */
	public void setResponseContent(String type, String body) {
		getResponse().setContent(type, body);
	}

	/**
	 * Sets the response body (plaintext)
	 * 
	 * @param body Response body string
	 */
	public void setResponseContent(String body) {
		setResponseContent("text/plain", body);
	}

	/**
	 * Sets the response body (binary)
	 * 
	 * @param type Content type
	 * @param body Response body bytes
	 */
	public void setResponseContent(String type, byte[] body) {
		getResponse().setContent(type, body);
	}

	/**
	 * Sets the response body (binary)
	 * 
	 * @param body Response body bytes
	 */
	public void setResponseContent(byte[] body) {
		getResponse().setContent(body);
	}

	/**
	 * Sets the response body (InputStream)
	 * 
	 * @param type Content type
	 * @param body Input stream
	 */
	public HttpResponse setResponseContent(String type, InputStream body) {
		return getResponse().setContent(type, body);
	}

	/**
	 * Sets the response body (InputStream)
	 * 
	 * @param body Input stream
	 */
	public HttpResponse setResponseContent(InputStream body) {
		return getResponse().setContent(body);
	}

	/**
	 * Sets the response body (InputStream)
	 * 
	 * @param type   Content type
	 * @param body   Input stream
	 * @param length Content length
	 */
	public HttpResponse setResponseContent(String type, InputStream body, long length) {
		return getResponse().setContent(type, body, length);
	}

	/**
	 * Sets the response body (InputStream)
	 * 
	 * @param body   Input stream
	 * @param length Content length
	 */
	public HttpResponse setResponseContent(InputStream body, long length) {
		return getResponse().setContent(body, length);
	}

	/**
	 * Assigns a new response status
	 * 
	 * @param status  New status code
	 * @param message New status message
	 */
	public HttpResponse setResponseStatus(int status, String message) {
		return response.setResponseStatus(status, message);
	}

	/**
	 * Retrieves the HTTP request object
	 * 
	 * @return HttpRequest instance
	 */
	public HttpRequest getRequest() {
		return request;
	}

	/**
	 * Retrieves the HTTP response object
	 * 
	 * @return HttpResponse instance
	 */
	public HttpResponse getResponse() {
		return response;
	}

	/**
	 * Retrieves the HTTP request path
	 * 
	 * @return Request path
	 */
	public String getRequestPath() {
		return getRequest().getRequestPath();
	}

	/**
	 * Retrieves the HTTP request query
	 * 
	 * @return Request query string
	 */
	public String getRequestQuery() {
		return getRequest().getRequestQuery();
	}

	/**
	 * Retrieves the map of request query parameters
	 * 
	 * @return Request query parameters
	 */
	public Map<String, String> getRequestQueryParameters() {
		return getRequest().getRequestQueryParameters();
	}

	/**
	 * Retrieves the unparsed request path string
	 * 
	 * @return HTTP raw request string
	 */
	public String getRawRequestResource() {
		return getRequest().getRawRequestResource();
	}

	/**
	 * Retrieves the request method
	 * 
	 * @return HTTP request method string
	 */
	public String getRequestMethod() {
		return getRequest().getRequestMethod();
	}

}
