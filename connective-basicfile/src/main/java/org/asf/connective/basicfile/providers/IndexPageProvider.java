package org.asf.connective.basicfile.providers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.headers.HeaderCollection;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * HTTP Index Page Provider
 * 
 * @author Sky Swimmer
 *
 */
public abstract class IndexPageProvider {

	private ConnectiveHttpServer server;
	private HttpResponse response;
	private HttpRequest request;

	private File[] indexFiles;
	private File[] indexDirectories;

	private String indexloc;

	/**
	 * Instantiates a new index page processor
	 * 
	 * @param server      Server to use
	 * @param request     HTTP request
	 * @param response    HTTP response
	 * @param files       Files to be listed
	 * @param directories Directories to be listed
	 * @param indexLoc    Path to the folder being indexed (relative to the virtual
	 *                    root)
	 * @return New IndexPageProvider configured for processing
	 */
	public IndexPageProvider instantiate(ConnectiveHttpServer server, HttpRequest request, HttpResponse response,
			File[] files, File[] directories, String indexLoc) {
		IndexPageProvider inst = createInstance();
		inst.server = server;
		inst.response = response;
		inst.request = request;
		inst.indexDirectories = directories;
		inst.indexFiles = files;
		inst.indexloc = indexLoc;
		return inst;
	}

	/**
	 * Gets the folder path indexed (relative to the root source directory on disk)
	 * 
	 * @return Folder path as string
	 */
	protected String getFolderPath() {
		return indexloc;
	}

	/**
	 * Gets the files to be listed
	 * 
	 * @return Array of files
	 */
	protected File[] getFiles() {
		return indexFiles;
	}

	/**
	 * Gets the directory to be listed
	 * 
	 * @return Array of directories
	 */
	protected File[] getDirectories() {
		return indexDirectories;
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
	 * Creates a new instance of this index page processor
	 * 
	 * @return New IndexPageProvider instance
	 */
	protected abstract IndexPageProvider createInstance();

	/**
	 * Processes the request.
	 * 
	 * @param path        Folder path
	 * @param method      Request methods, either POST, PUT or DELETE
	 * @param client      Remote client
	 * @param files       Files to be listed
	 * @param directories Directories to be listed
	 * @throws IOException If processing fails
	 */
	public abstract void process(String path, String method, RemoteClient client, File[] files, File[] directories)
			throws IOException;

}
