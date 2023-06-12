package org.asf.connective.standalone.configuration.context;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.basicfile.DocumentProcessor;
import org.asf.connective.basicfile.FileProviderContext;
import org.asf.connective.basicfile.providers.IDocumentPostProcessorProvider;
import org.asf.connective.basicfile.providers.extensions.IContextProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IContextRootProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IProcessorProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IRemoteClientProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IServerProviderExtension;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.connective.standalone.ConnectiveStandaloneMain;

import groovy.lang.Closure;

public class PostProcessorConfig {

	public ArrayList<IDocumentPostProcessorProvider> postProcessors = new ArrayList<IDocumentPostProcessorProvider>();

	public class ClosurePostProcessor
			implements IDocumentPostProcessorProvider, IContextProviderExtension, IContextRootProviderExtension,
			IProcessorProviderExtension, IRemoteClientProviderExtension, IServerProviderExtension {

		private Closure<?> postProcessorClosure;
		private boolean acceptNonHtml;

		public RemoteClient client;
		public ConnectiveHttpServer server;
		public DocumentProcessor documentProcessor;
		public String virtualRoot;
		public FileProviderContext context;

		private Consumer<String> writeCallback;

		public ClosurePostProcessor(Closure<?> postProcessorClosure, boolean acceptNonHtml) {
			this.postProcessorClosure = postProcessorClosure;
			this.acceptNonHtml = acceptNonHtml;
			postProcessorClosure.setDelegate(this);
		}

		@Override
		public boolean acceptNonHTML() {
			return acceptNonHtml;
		}

		@Override
		public IDocumentPostProcessorProvider createInstance() {
			return new ClosurePostProcessor((Closure<?>) postProcessorClosure.clone(), acceptNonHtml);
		}

		@Override
		public boolean match(HttpRequest request, String inputPath) {
			return true;
		}

		@Override
		public void process(String path, HttpRequest request, HttpResponse response, RemoteClient client,
				String method) {
			postProcessorClosure.call(path, request, response, client, method);
		}

		@Override
		public void provide(ConnectiveHttpServer server) {
			this.server = server;
		}

		@Override
		public void provide(RemoteClient client) {
			this.client = client;
		}

		@Override
		public void provide(DocumentProcessor processor) {
			this.documentProcessor = processor;
		}

		@Override
		public void provideVirtualRoot(String virtualRoot) {
			this.virtualRoot = virtualRoot;
		}

		@Override
		public void provide(FileProviderContext context) {
			this.context = context;
		}

		@Override
		public void setWriteCallback(Consumer<String> callback) {
			writeCallback = callback;
		}

		@Override
		public Consumer<String> getWriteCallback() {
			return writeCallback;
		}

	}

	/**
	 * Adds post processor
	 * 
	 * @param postProcessor Post processor to add
	 */
	public void PostProcessor(IDocumentPostProcessorProvider postProcessor) {
		postProcessors.add(postProcessor);
	}

	/**
	 * Adds post processor
	 * 
	 * @param postProcessorClassName Post processor class name
	 * @param initParams             Constructor parameters
	 */
	public void PostProcessor(String postProcessorClassName, Object... initParams) {
		try {
			Class<?> cls = Class.forName(postProcessorClassName, true, ConnectiveStandaloneMain.getModuleClassLoader());
			if (IDocumentPostProcessorProvider.class.isAssignableFrom(cls)) {
				try {
					PostProcessor((IDocumentPostProcessorProvider) cls
							.getConstructor(Stream.of(initParams).map(t -> t.getClass()).toArray(t -> new Class[t]))
							.newInstance(initParams));
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new IllegalArgumentException("Invalid post processor class name: " + postProcessorClassName
							+ ": no constructor matching the passed arguments");
				}
			} else
				throw new IllegalArgumentException(
						"Invalid post processor class name: " + postProcessorClassName + ": not a postProcessor type");
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Invalid post processor class name: " + postProcessorClassName);
		}
	}

	/**
	 * Adds post processor
	 * 
	 * @param postProcessorClosure Post processor closure to add
	 */
	public void PostProcessor(Closure<?> postProcessorClosure) {
		PostProcessor(new ClosurePostProcessor(postProcessorClosure, false));
	}

	/**
	 * Adds post processor
	 * 
	 * @param postProcessorClosure Post processor closure to add
	 * @param acceptNonHtml        True to accept non-html pages, false to only
	 *                             process HTML pages
	 */
	public void PostProcessor(Closure<?> postProcessorClosure, boolean acceptNonHtml) {
		PostProcessor(new ClosurePostProcessor(postProcessorClosure, acceptNonHtml));
	}

	public static PostProcessorConfig fromClosure(Closure<?> closure) {
		PostProcessorConfig conf = new PostProcessorConfig();
		closure.setDelegate(conf);
		closure.call();
		return conf;
	}

}
