package org.asf.connective.standalone.configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.ContentSource;
import org.asf.connective.RemoteClient;
import org.asf.connective.basicfile.FileProviderContextFactory;
import org.asf.connective.basicfile.providers.FileUploadHandlerProvider;
import org.asf.connective.basicfile.providers.IDocumentPostProcessorProvider;
import org.asf.connective.basicfile.providers.IFileAliasProvider;
import org.asf.connective.basicfile.providers.IFileRestrictionProvider;
import org.asf.connective.basicfile.util.BasicfileContentSource;
import org.asf.connective.handlers.HttpRequestHandler;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.connective.headers.HeaderCollection;
import org.asf.connective.standalone.ConnectiveStandaloneMain;
import org.asf.connective.standalone.configuration.context.ContextConfig;
import org.asf.connective.standalone.modules.IConnectiveModule;
import org.asf.connective.standalone.modules.ModuleManager;

import groovy.lang.Closure;

public class HostEntry {

	private Logger logger = LogManager.getLogger("CONNECTIVE STANDALONE");

	// Adapter settings
	public String adapterName;
	public Map<String, String> adapterConfiguration;

	// Server settings
	public ArrayList<ContentSource> contentSources = new ArrayList<ContentSource>();
	public ArrayList<HttpRequestHandler> handlers = new ArrayList<HttpRequestHandler>();
	public HeaderCollection defaultHeaders = new HeaderCollection();
	public String serverName;

	// Error pages
	public BiFunction<HttpResponse, HttpRequest, String> errorGenerator = new BiFunction<HttpResponse, HttpRequest, String>() {
		protected String htmlCache = null;

		@Override
		public String apply(HttpResponse response, HttpRequest request) {
			try {
				InputStream strm = ConnectiveHttpServer.class.getResource("/error.template.html").openStream();
				htmlCache = new String(strm.readAllBytes());
			} catch (Exception ex) {
				if (htmlCache == null)
					return "FATAL ERROR GENERATING PAGE: " + ex.getClass().getTypeName() + ": " + ex.getMessage();
			}

			String str = htmlCache;

			str = str.replace("%path%", request.getRequestPath());
			str = str.replace("%server-name%", response.getHeaderValue("Server"));
			str = str.replace("%error-status%", Integer.toString(response.getResponseCode()));
			str = str.replace("%error-message%", response.getResponseMessage());

			return str;
		}

	};

	/**
	 * Assigns the server name
	 * 
	 * @param name Server name string
	 */
	public void ServerName(String name) {
		serverName = name;
	}

	/**
	 * Assigns the server adapter
	 * 
	 * @param adapter           Adapter name
	 * @param propertiesClosure Closure containing the adapter properties
	 */
	public void Adapter(String adapter, Closure<?> propertiesClosure) {
		HashMap<String, String> properties = new HashMap<String, String>();
		propertiesClosure.setDelegate(new PropertyBridge(properties));
		propertiesClosure.call();
		adapterName = adapter;
		adapterConfiguration = properties;

		// Check adapter validity
		if (ConnectiveHttpServer.findAdapter(adapter) == null)
			throw new IllegalArgumentException("Invalid adapter name: " + adapter);
	}

	/**
	 * Assigns the server adapter
	 * 
	 * @param adapter    Adapter name
	 * @param properties Adapter properties
	 */
	public void Adapter(String adapter, Map<String, String> properties) {
		adapterName = adapter;
		adapterConfiguration = properties;

		// Check adapter validity
		if (ConnectiveHttpServer.findAdapter(adapter) == null)
			throw new IllegalArgumentException("Invalid adapter name: " + adapter);
	}

	/**
	 * Configures default headers
	 * 
	 * @param defaultHeadersConfigClosure Default header configuration closure
	 */
	public void DefaultHeaders(Closure<?> defaultHeadersConfigClosure) {
		DefaultHeaderConfig.addFromClosure(defaultHeadersConfigClosure, defaultHeaders);
	}

	/**
	 * Assigns the server content source
	 * 
	 * @param source Server content source
	 */
	public void ContentSource(ContentSource source) {
		contentSources.add(source);
	}

	/**
	 * Assigns the server content source
	 * 
	 * @param sourceClassName Server content source class name
	 * @param initParams      Constructor parameters
	 */
	public void ContentSource(String sourceClassName, Object... initParams) {
		try {
			Class<?> cls = Class.forName(sourceClassName, true, ConnectiveStandaloneMain.getModuleClassLoader());
			if (ContentSource.class.isAssignableFrom(cls)) {
				try {
					ContentSource((ContentSource) cls
							.getConstructor(Stream.of(initParams).map(t -> t.getClass()).toArray(t -> new Class[t]))
							.newInstance(initParams));
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new IllegalArgumentException("Invalid content source class name: " + sourceClassName
							+ ": no constructor matching the passed arguments");
				}
			} else
				throw new IllegalArgumentException(
						"Invalid content source class name: " + sourceClassName + ": not a content source type");
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Invalid content source class name: " + sourceClassName);
		}
	}

	/**
	 * Assigns the server content source
	 * 
	 * @param source Server content source closure
	 */
	public void ContentSource(Closure<Boolean> source) {
		ContentSource src = new ContentSource() {
			@Override
			public boolean process(String path, HttpRequest request, HttpResponse response, RemoteClient client,
					ConnectiveHttpServer server) throws IOException {
				return source.call(path, request, response, client, server);
			}
		};
		source.setDelegate(src);
		contentSources.add(src);
	}

	/**
	 * Configures server context
	 * 
	 * @param root                 Physical root path
	 * @param contextConfigClosure Context configuration closure
	 */
	public void FileContext(String root, Closure<?> contextConfigClosure) {
		FileContext(root, ContextConfig.fromClosure(contextConfigClosure));
	}

	/**
	 * Configures server context
	 * 
	 * @param root          Physical root path
	 * @param contextConfig Context configuration
	 */
	public void FileContext(String root, ContextConfig contextConfig) {
		// Add basicfile adapter if needed
		if (!contentSources.stream().anyMatch(t -> t instanceof BasicfileContentSource))
			contentSources.add(new BasicfileContentSource());
		BasicfileContentSource source = (BasicfileContentSource) contentSources.stream()
				.filter(t -> t instanceof BasicfileContentSource).findFirst().get();

		// Check and log
		if (contextConfig.virtualRoot == null)
			throw new IllegalArgumentException("No virtual root configured in context for " + root);
		logger.info("Creating server context: " + contextConfig.virtualRoot);

		// Configure context factory
		FileProviderContextFactory fac = new FileProviderContextFactory();
		try {
			File rootDir = new File(root);
			if (!rootDir.exists())
				if (!rootDir.mkdirs())
					throw new Exception();
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid physical root path: " + root);
		}
		fac.setFileSourceFolder(root);
		logger.info("Physical root set to " + root);

		// Configure context
		for (HttpRequestHandler proc : contextConfig.handlers) {
			logger.info("Registered handler: " + proc.getClass().getTypeName());
			fac.registerProcessor(proc);
		}
		for (IFileAliasProvider alias : contextConfig.aliases) {
			logger.info("Registered alias: " + alias.getClass().getTypeName());
			fac.registerAlias(alias);
		}
		for (IDocumentPostProcessorProvider postProcessor : contextConfig.postProcessors) {
			logger.info("Registered post-processor: " + postProcessor.getClass().getTypeName());
			fac.registerPostProcessor(postProcessor);
		}
		for (FileUploadHandlerProvider uploadHandler : contextConfig.uploadHandlers) {
			logger.info("Registered upload handler: " + uploadHandler.getClass().getTypeName());
			fac.registerUploadHandler(uploadHandler);
		}
		for (IFileRestrictionProvider restriction : contextConfig.restrictions) {
			logger.info("Registered restriction: " + restriction.getClass().getTypeName());
			fac.registerRestriction(restriction);
		}
		// TODO

		// Let modules register data
		for (IConnectiveModule module : ModuleManager.getLoadedModules()) {
			try {
				module.onPrepareContext(fac);
			} catch (Exception e) {
				logger.error("Failed to run module onPrepareContext for " + module.moduleID(), e);
			}
		}

		// Register
		source.registerContext(contextConfig.virtualRoot, fac.build());
	}

	/**
	 * Configures server handlers
	 * 
	 * @param handlerConfigClosure Server handler configuration closure
	 */
	public void Handlers(Closure<?> handlerConfigClosure) {
		Handlers(HandlerConfig.fromClosure(handlerConfigClosure));
	}

	/**
	 * Configures server handlers
	 * 
	 * @param handlerConfig Server handler configuration
	 */
	public void Handlers(HandlerConfig handlerConfig) {
		handlers.addAll(handlerConfig.handlers);
	}

	/**
	 * Configures the error page generator
	 * 
	 * @param closure Error page generator closure
	 */
	public void ErrorPageGenerator(Closure<String> closure) {
		errorGenerator = ((response, request) -> {
			return closure.call(request, response);
		});
	}

	/**
	 * Reads a file (utility method)
	 * 
	 * @param path Path to the file to read
	 * @return File content string or null
	 */
	public String ReadFileString(String path) {
		File f = new File(path);
		if (f.exists())
			try {
				return Files.readString(f.toPath());
			} catch (IOException e) {
				return null;
			}
		return null;
	}

	public static HostEntry fromClosure(Closure<?> closure) {
		HostEntry conf = new HostEntry();
		closure.setDelegate(conf);
		closure.call();
		return conf;
	}

}
