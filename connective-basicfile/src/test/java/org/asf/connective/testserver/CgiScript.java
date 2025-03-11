package org.asf.connective.testserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.NetworkedConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.basicfile.FileProviderContext;
import org.asf.connective.io.IoUtil;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * CGI Script Interface - tool to use CGI scripts in ASF Connective servers
 * 
 * @author Sky Swimmer
 *
 */
public class CgiScript {

	protected String CGI_VERSION = "1.1";

	protected ConnectiveHttpServer server;

	protected String binary = "";
	protected String[] baseArgs = new String[0];

	protected HashMap<String, Object> environment = new HashMap<String, Object>();
	protected ArrayList<CgiContentProvider> contentProviders = new ArrayList<CgiContentProvider>();

	protected CgiScript(ConnectiveHttpServer server) {
		this.server = server;
	}

	/**
	 * 
	 * CGI Content provider - provides PUT/POST body content.
	 * 
	 * @author Stefan0436 - AerialWorks Software Foundation
	 *
	 */
	public static interface CgiContentProvider {

		/**
		 * Transfers the body to the CGI script
		 * 
		 * @param output Output stream
		 * @throws IOException If transferring fails.
		 * @return True if content was transferred, false otherwise.
		 */
		public boolean transfer(OutputStream output) throws IOException;

		/**
		 * Closes the input
		 * 
		 * @throws IOException If closing fails
		 */
		public void finish() throws IOException;
	}

	/**
	 * 
	 * CGI Content Provider for HTTP Requests
	 * 
	 * @author Sky Swimmer
	 *
	 */
	public static class RequestContentProvider implements CgiContentProvider {

		protected HttpRequest request;

		public RequestContentProvider(HttpRequest request) {
			this.request = request;
		}

		@Override
		public boolean transfer(OutputStream output) throws IOException {
			request.transferRequestBody(output);
			return request.getBodyStream() != null;
		}

		@Override
		public void finish() throws IOException {
		}

	}

	/**
	 * 
	 * CGI Context
	 * 
	 * @author Sky Swimmer
	 *
	 */
	public static class CgiContext {
		protected Process cgiProcess;
		protected InputStream output;
		protected OutputStream input;
		protected ArrayList<CgiContentProvider> contentProviders;

		public Process getProcess() {
			return cgiProcess;
		}

		/**
		 * The CGI script's STDOUT
		 */
		public InputStream getOutput() {
			return output;
		}

		/**
		 * The CGI script's STDIN
		 */
		public OutputStream getInput() {
			return input;
		}

		/**
		 * Applies the CGI script to a given HTTP response. (will not set the body, only
		 * the headers)
		 * 
		 * @param request  HTTP request
		 * @param response HTTP response
		 * @throws IOException If applying the CGI output fails
		 */
		public void applyToResponse(HttpRequest request, HttpResponse response) throws IOException {
			boolean doClose = false;

			// Try to transfer content
			for (CgiContentProvider provider : contentProviders) {
				if (provider.transfer(getInput()))
					doClose = true;
			}

			// Check method
			if (doClose || (!request.getRequestMethod().equals("POST") && !request.getRequestMethod().equals("PUT"))) {
				cgiProcess.getOutputStream().flush();
				cgiProcess.getOutputStream().close();
			}

			// Read the headers
			HashMap<String, String> headersCache = new HashMap<String, String>();
			while (true) {
				String line = readStreamLine(getOutput());
				if (line.isEmpty())
					break;

				String key = null;
				try {
					key = line.substring(0, line.indexOf(":"));
				} catch (Exception ex) {

				}
				if (key == null) {
					break;
				}

				String value = "";
				try {
					value = line.substring(line.indexOf(": ") + 2);
				} catch (Exception ex) {

				}

				// Assign headers
				if (headersCache.containsKey(key)) {
					headersCache.put(key, value);
					response.addHeader(key, value, true);
				} else {
					headersCache.put(key, value);
					response.addHeader(key, value);
				}
			}

			// Set status
			if (response.getHeaders().hasHeader("Status")) {
				String status = response.getHeaderValue("Status");
				response.setResponseStatus(Integer.valueOf(status.substring(0, status.indexOf(" "))),
						status.substring(status.indexOf(" ") + 1));
				response.removeHeader("Status");
			}

			// Clean up
			for (CgiContentProvider provider : contentProviders) {
				provider.finish();
			}
		}

		/**
		 * Applies the CGI script to a given HTTP response and writes the body to the
		 * output stream
		 * 
		 * @param request  HTTP request
		 * @param response HTTP response
		 * @throws IOException If applying the CGI output fails
		 */
		public void applyFullCGI(HttpRequest request, HttpResponse response) throws IOException {
			// Assign content type if needed
			String type = response.getHeaders().getHeaderValue("Content-Type");
			if (type == null) {
				type = "text/html";
				response.addHeader("Content-Type", type);
			}

			// Apply response and transfer output
			applyToResponse(request, response);
			type = response.getHeaders().getHeaderValue("Content-Type");
			response.setContent(type, output);
		}

		/**
		 * Writes input to the CGI script
		 * 
		 * @param input  Input to transfer
		 * @param length Amount of bytes to write
		 * @throws IOException If writing fails
		 */
		public CgiContext writeInput(InputStream input, long length) throws IOException {
			return writeInput(input, length, false);
		}

		/**
		 * Writes input to the CGI script
		 * 
		 * @param input  Input to transfer
		 * @param length Amount of bytes to write (please send Content-Length to the
		 *               response as well)
		 * @param close  True to close the input stream, do this when you are finished
		 *               writing to the STDIN of the CGI script
		 * @throws IOException If writing fails
		 */
		public CgiContext writeInput(InputStream input, long length, boolean close) throws IOException {
			if (input == null)
				return this;
			int tr = 0;
			for (long i = 0; i < length; i += tr) {
				tr = Integer.MAX_VALUE / 1000;
				if ((length - (long) i) < tr) {
					tr = input.available();
					if (tr == 0) {
						getInput().write(input.read());
						i += 1;
					}
					tr = input.available();
				}
				getInput().write(IoUtil.readNBytes(input, tr));
			}

			if (close) {
				cgiProcess.getOutputStream().flush();
				cgiProcess.getOutputStream().close();
			}

			return this;
		}

		private String readStreamLine(InputStream strm) throws IOException {
			String buffer = "";
			while (true) {
				char ch = (char) strm.read();
				if (ch == (char) -1)
					return buffer;
				if (ch == '\n') {
					return buffer;
				} else if (ch != '\r') {
					buffer += ch;
				}
			}
		}
	}

	/**
	 * Creates a new instance of the CGI interface
	 * 
	 * @param server        HTTP Server instance
	 * @param executable    Binary to execute
	 * @param baseArguments Additional arguments needed to run the scripts
	 */
	public static CgiScript create(ConnectiveHttpServer server, String executable, String... baseArguments) {
		return new CgiScript(server).setExecutable(executable, baseArguments);
	}

	/**
	 * Sets the executable and base arguments for the CGI script.
	 * 
	 * @param executable    Binary to execute.
	 * @param baseArguments Additional arguments needed to run the script.
	 */
	public CgiScript setExecutable(String executable, String... baseArguments) {
		baseArgs = baseArguments;
		binary = executable;
		return this;
	}

	/**
	 * Sets the gateway interface specification version.
	 * 
	 * @param version CGI version.
	 */
	public CgiScript setCGIVersion(String version) {
		CGI_VERSION = version;
		return this;
	}

	/**
	 * Sets the variables for the basic file module
	 * 
	 * @param context Server context to use
	 * @param path    Script path relative to the context root
	 * @throws IOException If assigning fails
	 */
	public CgiScript setFileVariable(FileProviderContext context, String path) throws IOException {

		//
		// Assign the document root
		String root = new File(context.getWebrootFolderPath()).getCanonicalPath();
		setVariable("DOCUMENT_ROOT", root);

		//
		// Assign script variables
		File file = new File(root, path);
		setVariable("SCRIPT_NAME", path);
		setVariable("SCRIPT_FILENAME", file.getCanonicalPath());

		return this;
	}

	/**
	 * Sets the server variables
	 * 
	 * @param serverName Server name
	 */
	public CgiScript setServerVariables(String serverName) {

		//
		// CGI information
		setVariable("GATEWAY_INTERFACE", "CGI/" + CGI_VERSION);

		//
		// Server information
		setVariable("SERVER_NAME", serverName);
		setVariable("SERVER_SOFTWARE", server.getServerName() + " " + server.getServerVersion());
		setVariable("SERVER_PROTOCOL", server.getProtocolName());
		if (server instanceof NetworkedConnectiveHttpServer)
			setVariable("SERVER_PORT", ((NetworkedConnectiveHttpServer) server).getListenPort());

		return this;
	}

	/**
	 * Sets the request information variables
	 * 
	 * @param request HTTP request
	 */
	public CgiScript setRequestVariables(HttpRequest request) {

		// Request information
		setVariable("REQUEST_METHOD", request.getRequestMethod());
		setVariable("SCRIPT_NAME", request.getRequestPath());
		setVariable("QUERY_STRING", request.getRequestQuery());
		setVariable("PATH_INFO", ""); // TODO: reimplement

		//
		// Dynamic variables
		if (request.getHeaders().hasHeader("Accept"))
			setVariable("HTTP_ACCEPT", request.getHeaderValue("Accept"));
		if (request.getHeaders().hasHeader("Accept-Charset"))
			setVariable("HTTP_ACCEPT_CHARSET", request.getHeaderValue("Accept-Charset"));
		if (request.getHeaders().hasHeader("Accept-Encoding"))
			setVariable("HTTP_ACCEPT_ENCODING", request.getHeaderValue("Accept-Encoding"));
		if (request.getHeaders().hasHeader("Accept-Language"))
			setVariable("HTTP_ACCEPT_LANGUAGE", request.getHeaderValue("Accept-Language"));

		if (request.getHeaders().hasHeader("Content-Length"))
			setVariable("CONTENT_LENGTH", request.getHeaderValue("Content-Length"));
		if (request.getHeaders().hasHeader("Content-Type"))
			setVariable("CONTENT_TYPE", request.getHeaderValue("Content-Type"));

		if (request.getHeaders().hasHeader("Host"))
			setVariable("HTTP_HOST", request.getHeaderValue("Host"));
		if (request.getHeaders().hasHeader("Connection"))
			setVariable("HTTP_CONNECTION", request.getHeaderValue("Connection"));

		if (request.getHeaders().hasHeader("User-Agent"))
			setVariable("HTTP_USER_AGENT", request.getHeaderValue("User-Agent"));
		if (request.getHeaders().hasHeader("Cookie"))
			setVariable("HTTP_COOKIE", request.getHeaderValue("Cookie"));

		//
		// Authentication information
		if (request.getHeaders().hasHeader("Authorization")) {
			String header = request.getHeaderValue("Authorization");
			setVariable("AUTH_TYPE", header.substring(0, header.indexOf(" ")));
		}

		return this;
	}

	/**
	 * Sets all the default CGI Variables
	 * 
	 * @param serverName Server name
	 * @param request    HTTP Request
	 * @param client     Client making the request
	 */
	public CgiScript setDefaultVariables(String serverName, HttpRequest request, RemoteClient client) {
		this.setServerVariables(serverName);
		this.setRequestVariables(request);
		this.setRemoteInfoVariables(client);
		return this;
	}

	/**
	 * Assigns client information variables
	 * 
	 * @param client Client making the request
	 */
	public CgiScript setRemoteInfoVariables(RemoteClient client) {
		String address = client.getRemoteAddress();
		String host = client.getRemoteHost();
		setVariable("REMOTE_HOST", host);
		setVariable("REMOTE_ADDR", address);
		return this;
	}

	/**
	 * Runs the script
	 * 
	 * @return Script execution context
	 * @throws IOException If starting fails
	 */
	public CgiContext run() throws IOException {
		ProcessBuilder builder = new ProcessBuilder();
		String[] cmd = new String[1 + baseArgs.length];
		cmd[0] = binary;
		for (int i = 0; i < baseArgs.length; i++)
			cmd[i + 1] = baseArgs[i];
		builder.command(cmd);

		environment.forEach((k, v) -> {
			builder.environment().put(k, v.toString());
		});

		CgiContext context = new CgiContext();
		context.contentProviders = new ArrayList<CgiContentProvider>();
		context.contentProviders.addAll(contentProviders);

		Process proc = builder.start();
		context.cgiProcess = proc;
		context.input = proc.getOutputStream();
		context.output = proc.getInputStream();

		return context;
	}

	/**
	 * Adds content providers to the CGI script
	 * 
	 * @param provider Content provider to add
	 */
	public CgiScript addContentProvider(CgiContentProvider provider) {
		contentProviders.add(provider);
		return this;
	}

	/**
	 * Transfers the HTTP body to the CGI script when it starts
	 * 
	 * @param request HTTP request
	 */
	public CgiScript addContentProvider(HttpRequest request) {
		return addContentProvider(new RequestContentProvider(request));
	}

	/**
	 * Sets the given variable (converted to string)
	 * 
	 * @param variable Variable name
	 * @param value    Value to set
	 */
	public <T> CgiScript setVariable(String variable, T value) {
		environment.put(variable, value);
		return this;
	}

}
