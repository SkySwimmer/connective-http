package org.asf.connective;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.asf.connective.headers.HeaderCollection;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.connective.processors.HttpRequestProcessor;
import org.asf.connective.impl.DelegatePushProcessor;
import org.asf.connective.impl.DelegateRequestProcessor;
import org.asf.connective.impl.http_1_1.Http_1_1_Adapter;
import org.asf.connective.impl.https_1_1.Https_1_1_Adapter;
import org.asf.connective.io.IoUtil;
import org.asf.connective.lambda.LambdaPushProcessor;
import org.asf.connective.lambda.LambdaRequestProcessor;

/**
 * 
 * Connective HTTP server abstract
 * 
 * @author Sky Swimmer
 *
 */
public abstract class ConnectiveHttpServer {
	/**
	 * Version of the ConnectiveHTTP library
	 */
	public static final String CONNECTIVE_VERSION = "1.0.0.A16";

	private ContentSource contentSource = new DefaultContentSource();
	private Logger logger = LogManager.getLogger("connective-http");
	private HeaderCollection defaultHeaders = new HeaderCollection();

	protected boolean processorsRequiresResort;
	protected ArrayList<HttpRequestProcessor> processors = new ArrayList<HttpRequestProcessor>();

	protected ArrayList<String> allowedProxySourceAddresses = new ArrayList<String>();

	public ConnectiveHttpServer() {
		// Proxies
		for (String addr : System.getProperty("connectiveAllowedProxies", "").replace(" ", "").split(","))
			addAllowedProxySources(addr);
		for (String addr : System.getProperty("connectiveAllowProxies", "").replace(" ", "").split(","))
			addAllowedProxySources(addr);
		for (String addr : System.getProperty("connectiveGrantProxies", "").replace(" ", "").split(","))
			addAllowedProxySources(addr);
	}

	/**
	 * Retrieves the list of allowed addresses for proxy processing, this list
	 * controls which proxies are authorized to update the client addresses
	 * 
	 * @return Array of allowed proxy source addresses
	 */
	public String[] getAllowedProxySourceAddresses() {
		while (true) {
			try {
				return allowedProxySourceAddresses.toArray(new String[0]);
			} catch (ConcurrentModificationException e) {
			}
		}
	}

	/**
	 * Checks if a specific proxy is allowed to update the client addresses
	 * 
	 * @param address Address of the proxy to verify
	 * @return True if allowed, false otherwise
	 */
	public boolean isAllowedProxySource(String address) {
		while (true) {
			try {
				return allowedProxySourceAddresses.stream().anyMatch(t -> t.toLowerCase().equals(address));
			} catch (ConcurrentModificationException e) {
			}
		}
	}

	/**
	 * Adds allowed proxy source addresses
	 * 
	 * @param address The address of the proxy server to whitelist
	 */
	public void addAllowedProxySources(String address) {
		synchronized (allowedProxySourceAddresses) {
			allowedProxySourceAddresses.add(address);
		}
	}

	/**
	 * Removes allowed proxy source addresses
	 * 
	 * @param address The address of the proxy server to remove from the whitelist
	 */
	public void removeAllowedProxySources(String address) {
		synchronized (allowedProxySourceAddresses) {
			allowedProxySourceAddresses.remove(address);
		}
	}

	/**
	 * Clears all allowed proxy source addresses
	 */
	public void clearAllowedProxySources() {
		synchronized (allowedProxySourceAddresses) {
			allowedProxySourceAddresses.clear();
		}
	}

	/**
	 * Retrieves all registered HTTP request processors
	 * 
	 * @return Array of HttpRequestProcessor instances
	 */
	public HttpRequestProcessor[] getAllRequestProcessors() {
		resortProcessorsIfNeeded();
		return processors.toArray(new HttpRequestProcessor[0]);
	}

	/**
	 * @deprecated No longer supported, use getAllRequestProcessors() instead
	 */
	@Deprecated
	public HttpPushProcessor[] getRequestProcessors() {
		resortProcessorsIfNeeded();
		return processors.stream().filter(t -> !(t instanceof HttpPushProcessor))
				.toArray(t -> new HttpPushProcessor[t]);
	}

	/**
	 * @deprecated No longer supported, use getAllRequestProcessors() instead
	 */
	@Deprecated
	public HttpPushProcessor[] getPushProcessors() {
		resortProcessorsIfNeeded();
		return processors.stream().filter(t -> t instanceof HttpPushProcessor).toArray(t -> new HttpPushProcessor[t]);
	}

	private static ArrayList<IServerAdapterDefinition> adapters = new ArrayList<IServerAdapterDefinition>(
			Arrays.asList(new IServerAdapterDefinition[] { new Http_1_1_Adapter(), new Https_1_1_Adapter() }));

	private BiFunction<HttpResponse, HttpRequest, String> errorGenerator = new BiFunction<HttpResponse, HttpRequest, String>() {
		protected String htmlCache = null;

		@Override
		public String apply(HttpResponse response, HttpRequest request) {
			try {
				InputStream strm = getClass().getResource("/error.template.html").openStream();
				htmlCache = new String(IoUtil.readAllBytes(strm));
				strm.close();
			} catch (Exception ex) {
				if (htmlCache == null)
					return "FATAL ERROR GENERATING PAGE: " + ex.getClass().getTypeName() + ": " + ex.getMessage();
			}

			String str = htmlCache;

			str = str.replace("%path%", request.getRequestPath());
			str = str.replace("%server-name%", getServerName());
			str = str.replace("%server-version%", getServerVersion());
			str = str.replace("%error-status%", Integer.toString(response.getResponseCode()));
			str = str.replace("%error-message%", response.getResponseMessage());

			return str;
		}

	};

	/**
	 * Retrieves the current ContentSource instance
	 * 
	 * @return ContentSource instance
	 */
	public ContentSource getContentSource() {
		return contentSource;
	}

	/**
	 * Assigns the ContentSource instance used by the server
	 * 
	 * @param newSource New ContentSource instance to handle server requests
	 */
	public void setContentSource(ContentSource newSource) {
		newSource.parent = contentSource;
		contentSource = newSource;
	}

	/**
	 * Registers adapters
	 * 
	 * @param adapter Adapter to register
	 */
	public static void registerAdapter(IServerAdapterDefinition adapter) {
		synchronized (adapters) {
			adapters.add(adapter);
		}
	}

	/**
	 * Finds adapters by name
	 * 
	 * @param adapterName Adapter name
	 * @return IServerAdapterDefinition instance or null
	 */
	public static IServerAdapterDefinition findAdapter(String adapterName) {
		IServerAdapterDefinition[] adapterLst;
		synchronized (adapters) {
			adapterLst = adapters.toArray(new IServerAdapterDefinition[0]);
		}
		for (IServerAdapterDefinition adapter : adapterLst) {
			if (adapter != null) {
				if (adapter.getName().equalsIgnoreCase(adapterName))
					return adapter;
			}
		}
		return null;
	}

	/**
	 * Creates a server instance by adapter name
	 * 
	 * @param adapterName Adapter name
	 * @return ConnectiveHttpServer instance or null if not found
	 * @throws IllegalArgumentException If the configuration is invalid
	 */
	public static ConnectiveHttpServer create(String adapterName) throws IllegalArgumentException {
		return create(adapterName, new HashMap<String, String>());
	}

	/**
	 * Creates a server instance by adapter name and configuration
	 * 
	 * @param adapterName   Adapter name
	 * @param configuration Server configuration
	 * @return ConnectiveHttpServer instance or null if not found
	 * @throws IllegalArgumentException If the configuration is invalid
	 */
	public static ConnectiveHttpServer create(String adapterName, Map<String, String> configuration)
			throws IllegalArgumentException {
		IServerAdapterDefinition adapter = findAdapter(adapterName);
		if (adapter == null)
			return null;
		ConnectiveHttpServer srv = adapter.createServer(configuration);

		// Update
		if (configuration.containsKey("allowed-proxies")) {
			for (String addr : configuration.get("allowed-proxies").replace(" ", "").split(",")) {
				srv.addAllowedProxySources(addr);
			}
		}
		if (configuration.containsKey("Allowed-proxies")) {
			for (String addr : configuration.get("Allowed-proxies").replace(" ", "").split(",")) {
				srv.addAllowedProxySources(addr);
			}
		}
		if (configuration.containsKey("Allowed-Proxies")) {
			for (String addr : configuration.get("Allowed-Proxies").replace(" ", "").split(",")) {
				srv.addAllowedProxySources(addr);
			}
		}
		if (configuration.containsKey("allow-proxy")) {
			for (String addr : configuration.get("allow-proxy").replace(" ", "").split(",")) {
				srv.addAllowedProxySources(addr);
			}
		}
		if (configuration.containsKey("Allow-proxy")) {
			for (String addr : configuration.get("Allow-proxy").replace(" ", "").split(",")) {
				srv.addAllowedProxySources(addr);
			}
		}
		if (configuration.containsKey("Allow-Proxy")) {
			for (String addr : configuration.get("Allow-Proxy").replace(" ", "").split(",")) {
				srv.addAllowedProxySources(addr);
			}
		}

		// Return
		return srv;
	}

	/**
	 * Creates a networked server instance by adapter name
	 * 
	 * @param adapterName Adapter name
	 * @return NetworkedConnectiveHttpServer instance or null if not found
	 * @throws IllegalArgumentException If the configuration is invalid
	 */
	public static NetworkedConnectiveHttpServer createNetworked(String adapterName) throws IllegalArgumentException {
		return createNetworked(adapterName, new HashMap<String, String>());
	}

	/**
	 * Creates a networked server instance by adapter name and configuration
	 * 
	 * @param adapterName   Adapter name
	 * @param configuration Server configuration
	 * @return NetworkedConnectiveHttpServer instance or null if not found
	 * @throws IllegalArgumentException If the configuration is invalid
	 */
	public static NetworkedConnectiveHttpServer createNetworked(String adapterName, Map<String, String> configuration)
			throws IllegalArgumentException {
		IServerAdapterDefinition adapter = findAdapter(adapterName);
		if (adapter == null)
			return null;
		ConnectiveHttpServer srv = adapter.createServer(configuration);

		// Update
		if (configuration.containsKey("allowed-proxies")) {
			for (String addr : configuration.get("allowed-proxies").replace(" ", "").split(",")) {
				srv.addAllowedProxySources(addr);
			}
		}
		if (configuration.containsKey("Allowed-proxies")) {
			for (String addr : configuration.get("Allowed-proxies").replace(" ", "").split(",")) {
				srv.addAllowedProxySources(addr);
			}
		}
		if (configuration.containsKey("Allowed-Proxies")) {
			for (String addr : configuration.get("Allowed-Proxies").replace(" ", "").split(",")) {
				srv.addAllowedProxySources(addr);
			}
		}
		if (configuration.containsKey("allow-proxy")) {
			for (String addr : configuration.get("allow-proxy").replace(" ", "").split(",")) {
				srv.addAllowedProxySources(addr);
			}
		}
		if (configuration.containsKey("Allow-proxy")) {
			for (String addr : configuration.get("Allow-proxy").replace(" ", "").split(",")) {
				srv.addAllowedProxySources(addr);
			}
		}
		if (configuration.containsKey("Allow-Proxy")) {
			for (String addr : configuration.get("Allow-Proxy").replace(" ", "").split(",")) {
				srv.addAllowedProxySources(addr);
			}
		}

		// Return
		if (srv instanceof NetworkedConnectiveHttpServer)
			return (NetworkedConnectiveHttpServer) srv;
		return null;
	}

	/**
	 * Retrieves the client logger
	 * 
	 * @return Logger instance
	 */
	protected Logger getLogger() {
		return logger;
	}

	/**
	 * Retrieves the server name
	 * 
	 * @return Server name string
	 */
	public abstract String getServerName();

	/**
	 * Retrieves the server version
	 * 
	 * @return Server version string
	 */
	public abstract String getServerVersion();

	/**
	 * Re-assigns the HTTP server name to a custom value
	 * 
	 * @param name HTTP server name
	 */
	public abstract void setServerName(String name);

	/**
	 * Starts the HTTP server
	 * 
	 * @throws IOException If starting fails
	 */
	public abstract void start() throws IOException;

	/**
	 * Stops the HTTP server
	 * 
	 * @throws IOException If stopping the server fails
	 */
	public abstract void stop() throws IOException;

	/**
	 * Stops the HTTP server without waiting for all clients to disconnect
	 * 
	 * @throws IOException If stopping the server fails
	 */
	public abstract void stopForced() throws IOException;

	/**
	 * Checks if the server is running
	 * 
	 * @return True if running, false otherwise
	 */
	public abstract boolean isRunning();

	/**
	 * Retrieves the default server headers
	 * 
	 * @return Default header collection
	 */
	public HeaderCollection getDefaultHeaders() {
		return defaultHeaders;
	}

	/**
	 * Waits for the server to shut down
	 */
	public void waitForExit() {
		while (isRunning()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	/**
	 * Registers a new request processor
	 * 
	 * @param path      The path to register to
	 * @param processor Processor call
	 */
	public void registerProcessor(String path, LambdaRequestProcessor processor) {
		registerProcessor(path, processor, false);
	}

	/**
	 * Registers a new request processor
	 * 
	 * @param path               The path to register to
	 * @param processor          Processor call
	 * @param supportsChildPaths True to supports child paths, false otherwise
	 */
	public void registerProcessor(String path, LambdaRequestProcessor processor, boolean supportsChildPaths) {
		registerProcessor(new DelegateRequestProcessor(path, processor, supportsChildPaths));
	}

	/**
	 * Registers a new push processor
	 * 
	 * @param path      The path to register to
	 * @param processor Processor call
	 */
	public void registerProcessor(String path, LambdaPushProcessor processor) {
		registerProcessor(path, processor, false);
	}

	/**
	 * Registers a new push processor
	 * 
	 * @param path               The path to register to
	 * @param processor          Processor call
	 * @param supportsChildPaths True to supports child paths, false otherwise
	 */
	public void registerProcessor(String path, LambdaPushProcessor processor, boolean supportsChildPaths) {
		registerProcessor(path, processor, supportsChildPaths, false);
	}

	/**
	 * Registers a new push processor
	 * 
	 * @param path               The path to register to
	 * @param processor          Processor call
	 * @param supportsChildPaths True to supports child paths, false otherwise
	 * @param supportsNonPush    True to support non-upload requests, false
	 *                           otherwise
	 */
	public void registerProcessor(String path, LambdaPushProcessor processor, boolean supportsChildPaths,
			boolean supportsNonPush) {
		registerProcessor(new DelegatePushProcessor(path, processor, supportsChildPaths, supportsNonPush));
	}

	/**
	 * Registers a new push processor
	 * 
	 * @param processor The processor implementation to register
	 */
	public void registerProcessor(HttpPushProcessor processor) {
		if (!processors.stream()
				.anyMatch(t -> t instanceof HttpPushProcessor
						&& t.getClass().getTypeName().equals(processor.getClass().getTypeName())
						&& t.supportsChildPaths() == processor.supportsChildPaths()
						&& ((HttpPushProcessor) t).supportsNonPush() == processor.supportsNonPush()
						&& t.path() == processor.path())) {
			processors.add(processor);
			processorsRequiresResort = true;
		}
	}

	/**
	 * Registers a new request processor
	 * 
	 * @param processor The processor implementation to register.
	 */
	public void registerProcessor(HttpRequestProcessor processor) {
		if (processor instanceof HttpPushProcessor) {
			registerProcessor((HttpPushProcessor) processor);
			return;
		}
		if (!processors.stream().anyMatch(t -> t.getClass().getTypeName().equals(processor.getClass().getTypeName())
				&& t.supportsChildPaths() == processor.supportsChildPaths() && t.path() == processor.path())) {
			processors.add(processor);
			processorsRequiresResort = true;
		}
	}

	/**
	 * Retrieves the error page generator
	 * 
	 * @return Error page generator
	 */
	public BiFunction<HttpResponse, HttpRequest, String> getErrorPageGenerator() {
		return errorGenerator;
	}

	/**
	 * Assigns the error page generator
	 * 
	 * @param errorGenerator New error page generator
	 */
	public void setErrorPageGenerator(BiFunction<HttpResponse, HttpRequest, String> errorGenerator) {
		this.errorGenerator = errorGenerator;
	}

	/**
	 * Retrieves the protocol name
	 * 
	 * @return Server protocol name
	 */
	public abstract String getProtocolName();

	/**
	 * Re-sorts all processors, required to properly process requests
	 */
	protected void resortProcessorsIfNeeded() {
		if (processorsRequiresResort) {
			// Resort
			processors.sort((t1, t2) -> {
				return -Integer.compare(sanitizePath(t1.path()).split("/").length,
						sanitizePath(t2.path()).split("/").length);
			});
		}
		processorsRequiresResort = false;
	}

	private String sanitizePath(String path) {
		if (path.contains("\\"))
			path = path.replace("\\", "/");
		while (path.startsWith("/"))
			path = path.substring(1);
		while (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		while (path.contains("//"))
			path = path.replace("//", "/");
		if (!path.startsWith("/"))
			path = "/" + path;
		return path;
	}

}
