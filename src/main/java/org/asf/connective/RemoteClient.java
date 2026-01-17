package org.asf.connective;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.asf.connective.logger.ConnectiveLogMessage;
import org.asf.connective.logger.ConnectiveLogger;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * Remote client connected with the server
 * 
 * @author Sky Swimmer
 *
 */
public abstract class RemoteClient {
	private ConnectiveLogger logger;

	private ConnectiveHttpServer server;

	protected RemoteClient(ConnectiveHttpServer server) {
		this.server = server;
		this.logger = server.getLogger().getManager().getLogger("client");
	}

	/**
	 * Retrieves the addresses of each proxy server in order, the leftmost address
	 * is the most recent server whereas the rightmost address is the first proxy
	 * that forwarded to the next
	 * 
	 * @return Array of proxy server addresses
	 */
	public abstract String[] getProxyChain();

	/**
	 * Retrieves the proxied client address, proxies are able to send the original
	 * client address to the server, this field will receive the address for both
	 * authoritive and non-authoritive proxies (<b>please note that this is not
	 * authoritive!</b> Use getAddress() to get the authoritive client address,
	 * whitelisted proxy servers will be able to replace the address with the
	 * proxied address)
	 * 
	 * @return Proxied client address
	 */
	public abstract String getRemoteProxiedClientAddress();

	/**
	 * Checks if a request was proxied
	 * 
	 * @return True if the request was proxied, false otherwise
	 */
	public boolean isProxied() {
		return getProxyChain().length != 0;
	}

	/**
	 * Verifies if the most recent proxy was authoritive
	 * 
	 * @return True if authoritive, false otherwise
	 */
	public boolean passedThroughAuthoritiveProxy() {
		return getRemoteProxiedClientAddress().equals(getRemoteAddress());
	}

	/**
	 * Retrieves the client logger
	 * 
	 * @return Logger instance
	 */
	public ConnectiveLogger getLogger() {
		return logger;
	}

	/**
	 * Processes HTTP requests
	 * 
	 * @param request HTTP request to process
	 * @throws IOException If processing fails
	 */
	public void processRequest(HttpRequest request) throws IOException {
		// Prepare response
		HttpResponse resp = createResponse(request);
		try {
			// Go through handler layers
			boolean run = true;
			ConnectiveHttpServer.LayerInfo layer = server.layer;
			while (layer != null) {
				if (!layer.proc.handle(request.getRequestPath(), request, resp, this, server)) {
					// Cancel handling
					run = false;
					break;
				}
				layer = layer.parent;
			}

			// Process if allowed
			if (!run || !server.getContentSource().process(request.getRequestPath(), request, resp, this, server)) {
				if (!request.getRequestMethod().equals("GET") && !request.getRequestMethod().equals("PUT")
						&& !request.getRequestMethod().equals("DELETE") && !request.getRequestMethod().equals("PATCH")
						&& !request.getRequestMethod().equals("POST") && !request.getRequestMethod().equals("HEAD")) {
					resp.setResponseStatus(405, "Unsupported request");
					logger.error(new ConnectiveLogMessage("handler",
							resp.getHttpVersion() + " " + request.getRequestMethod() + " "
									+ request.getRawRequestResource() + " : " + resp.getResponseCode() + " "
									+ resp.getResponseMessage(),
							null, this));
				} else {
					resp.setResponseStatus(404, "Not found");
					logger.error(new ConnectiveLogMessage("handler",
							resp.getHttpVersion() + " " + request.getRequestMethod() + " "
									+ request.getRawRequestResource() + " : " + resp.getResponseCode() + " "
									+ resp.getResponseMessage(),
							null, this));
				}
			} else {
				if (!resp.isSuccessResponseCode())
					logger.error(new ConnectiveLogMessage("handler",
							resp.getHttpVersion() + " " + request.getRequestMethod() + " "
									+ request.getRawRequestResource() + " : " + resp.getResponseCode() + " "
									+ resp.getResponseMessage(),
							null, this));
				else
					logger.info(new ConnectiveLogMessage("handler",
							resp.getHttpVersion() + " " + request.getRequestMethod() + " "
									+ request.getRawRequestResource() + " : " + resp.getResponseCode() + " "
									+ resp.getResponseMessage(),
							null, this));
			}

			// Set body if missing
			if (!resp.hasResponseBody()) {
				if (!resp.isSuccessResponseCode()) {
					// Set error
					resp.setContent("text/html", server.getErrorPageGenerator().apply(resp, request));
				}
			}

			// Send response
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			resp.addHeader("Date", dateFormat.format(new Date()));
			postProcessResponse(resp, request);
			if (!resp.wasStatusAssigned() && !resp.hasResponseBody() && resp.isSuccessResponseCode()) {
				// Set 204
				resp.setResponseStatus(204, "No Content");
			}
			sendResponse(resp, request);
		} finally {
			// If needed, we should close the response stream if its present to prevent
			// resource leakage
			if (resp.getBodyStream() != null) {
				try {
					resp.getBodyStream().close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Called to create a HTTP response object
	 * 
	 * @return HttpResponse instance
	 */
	protected abstract HttpResponse createResponseInternal();

	/**
	 * Creates a HTTP response
	 * 
	 * @param request Request object
	 * @return New HttpResponse instance
	 */
	protected HttpResponse createResponse(HttpRequest request) {
		HttpResponse resp = createResponseInternal();
		resp.addHeader("Server", server.getServerName());
		for (String name : server.getDefaultHeaders().getHeaderNames())
			if (!resp.hasHeader(name))
				resp.addHeader(name, server.getDefaultHeaders().getHeaderValue(name));
		return resp;
	}

	/**
	 * Called to post-process responses before sending
	 * 
	 * @param resp    Response object
	 * @param request Request object
	 */
	protected abstract void postProcessResponse(HttpResponse resp, HttpRequest request);

	/**
	 * Sends a HTTP response
	 * 
	 * @param response      Response to send back
	 * @param sourceRequest The request that prompted the response
	 * @throws IOException If sending the response fails
	 */
	protected abstract void sendResponse(HttpResponse response, HttpRequest sourceRequest) throws IOException;

	/**
	 * Retrieves the address of the client connected to the server
	 * 
	 * @return Remote IP address
	 */
	public abstract String getRemoteAddress();

	/**
	 * Retrieves the hostname of the client connected to the server
	 * 
	 * @return Remote hostname
	 */
	public abstract String getRemoteHost();

	/**
	 * Retrieves the client output stream
	 * 
	 * @return Client OutputStream instance
	 */
	public abstract OutputStream getOutputStream();

	/**
	 * Retrieves the client input stream
	 * 
	 * @return Client InputStream instance
	 */
	public abstract InputStream getInputStream();

	/**
	 * Checks if the client is still connected
	 * 
	 * @return True if connected, false otherwise
	 */
	public abstract boolean isConnected();

	/**
	 * Closes the connection
	 */
	public abstract void closeConnection();

	/**
	 * Retrieves the HTTP server associated with this client
	 * 
	 * @return ConnectiveHttpServer instance
	 */
	public ConnectiveHttpServer getServer() {
		return server;
	}

}
