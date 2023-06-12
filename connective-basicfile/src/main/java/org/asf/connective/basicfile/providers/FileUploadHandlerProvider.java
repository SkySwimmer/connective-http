package org.asf.connective.basicfile.providers;

import java.io.File;
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

/**
 * 
 * File Upload Handler - Support for methods like POST, PUT, DELETE.
 * 
 * @author Sky Swimmer
 *
 */
public abstract class FileUploadHandlerProvider {

	private ConnectiveHttpServer server;
	private HttpResponse response;
	private HttpRequest request;
	private File source;
	private String loc;

	/**
	 * Instantiates the upload handler
	 * 
	 * @param server   Server to use
	 * @param request  HTTP request
	 * @param response HTTP response
	 * @param location Path of the file relative to the virtual root
	 * @param source   Source file
	 * @return New FileUploadHandlerProvider configured for processing
	 */
	public FileUploadHandlerProvider instantiate(ConnectiveHttpServer server, HttpRequest request,
			HttpResponse response, String location, File source) {
		FileUploadHandlerProvider inst = createInstance();
		inst.server = server;
		inst.response = response;
		inst.request = request;
		inst.loc = location;
		inst.source = source;
		return inst;
	}

	/**
	 * Retrieves the source file
	 */
	protected File getSourceFile() {
		return source;
	}

	/**
	 * Gets the REAL source file path (relative to the root source directory on
	 * disk)
	 * 
	 * @return Source file path as string
	 */
	protected String getSourceFilePath() {
		return loc;
	}

	/**
	 * Checks if this processor supports directories, false will let the default
	 * system handle the directory.
	 */
	public boolean supportsDirectories() {
		return false;
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
	 * @param body Response body bytes
	 */
	protected void setResponseContent(byte[] body) {
		getResponse().setContent(body);
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
	protected HttpRequest getRequest() {
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

	/**
	 * Creates a new instance of this upload handler
	 * 
	 * @return New FileUploadHandlerProvider instance
	 */
	protected abstract FileUploadHandlerProvider createInstance();

	/**
	 * Checks if the given request can be handled by this upload handler
	 * 
	 * @param request   Request instance
	 * @param inputPath Request path (potentially aliased by another alias)
	 * @param method    Request methods, either POST, PUT or DELETE
	 * @return True if the request alias is valid, false otherwise
	 */
	public abstract boolean match(HttpRequest request, String inputPath, String method);

	/**
	 * Processes the request.
	 * 
	 * @param file        File object describing the file on disk
	 * @param path        File path
	 * @param method      Request methods, either POST, PUT or DELETE
	 * @param client      Remote client
	 * @param contentType Body content type
	 * @throws IOException If processing fails
	 */
	public abstract void process(File file, String path, String method, RemoteClient client, String contentType)
			throws IOException;

}
