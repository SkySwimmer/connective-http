package org.asf.connective;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.asf.connective.handlers.HttpHandlerSet;
import org.asf.connective.headers.HeaderCollection;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.connective.impl.http_1_1.Http_1_1_Adapter;
import org.asf.connective.impl.https_1_1.Https_1_1_Adapter;
import org.asf.connective.io.IoUtil;
import org.asf.connective.logger.ConnectiveLogger;
import org.asf.connective.logger.ConnectiveLoggerManager;

/**
 * 
 * Connective HTTP server abstract
 * 
 * @author Sky Swimmer
 *
 */
public abstract class ConnectiveHttpServer extends HttpHandlerSet {
	/**
	 * Version of the ConnectiveHTTP library
	 */
	public static final String CONNECTIVE_VERSION = "1.0.0.A19";

	static class LayerInfo {
		public IHttpHandlerLayer proc;
		public LayerInfo parent;
	}

	LayerInfo layer = null;
	private ContentSource contentSource = new DefaultContentSource(this);

	private ConnectiveLogger logger;
	private HeaderCollection defaultHeaders = new HeaderCollection();

	protected ArrayList<String> allowedProxySourceAddresses = new ArrayList<String>();

	private static ArrayList<IServerAdapterDefinition> adapters = new ArrayList<IServerAdapterDefinition>(
			Arrays.asList(new IServerAdapterDefinition[] { new Http_1_1_Adapter(), new Https_1_1_Adapter() }));

	public ConnectiveHttpServer() {
		// Proxies
		for (String addr : System.getProperty("connectiveAllowedProxies", "").replace(" ", "").split(","))
			addAllowedProxySources(addr);
		for (String addr : System.getProperty("connectiveAllowProxies", "").replace(" ", "").split(","))
			addAllowedProxySources(addr);
		for (String addr : System.getProperty("connectiveGrantProxies", "").replace(" ", "").split(","))
			addAllowedProxySources(addr);
		logger = ConnectiveLoggerManager.getInstance().getLogger("server");
	}

	/**
	 * Reassigns the logger implementation
	 * 
	 * @param logger New ConnectiveLogger instance
	 */
	public void setLogger(ConnectiveLogger logger) {
		this.logger = logger;
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
	 * Adds HTTP handler layers
	 * 
	 * @param layer Layer to add
	 * @since Connective 1.0.0.A17
	 */
	public void addHandlerLayer(IHttpHandlerLayer layer) {
		LayerInfo l = new LayerInfo();
		l.proc = layer;
		l.parent = this.layer;
		this.layer = l;
	}

	/**
	 * Retrieves all handler layers
	 * 
	 * @return Array of IHttpHandlerLayer instances
	 * @since Connective 1.0.0.A17
	 */
	public IHttpHandlerLayer[] getHandlerLayer() {
		ArrayList<IHttpHandlerLayer> handlers = new ArrayList<IHttpHandlerLayer>();
		LayerInfo l = layer;
		while (l != null) {
			handlers.add(l.proc);
			l = l.parent;
		}
		return handlers.toArray(new IHttpHandlerLayer[0]);
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
	 * @return ConnectiveLogger instance
	 */
	protected ConnectiveLogger getLogger() {
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
	public abstract void waitForExit();

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

}
