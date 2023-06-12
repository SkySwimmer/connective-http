package org.asf.connective.standalone.configuration.context;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.basicfile.DocumentProcessor;
import org.asf.connective.basicfile.FileProviderContext;
import org.asf.connective.basicfile.providers.IFileAliasProvider;
import org.asf.connective.basicfile.providers.extensions.IContextProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IContextRootProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IProcessorProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IRemoteClientProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IServerProviderExtension;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.standalone.ConnectiveStandaloneMain;

import groovy.lang.Closure;

public class AliasConfig {

	public ArrayList<IFileAliasProvider> aliases = new ArrayList<IFileAliasProvider>();

	public class ClosureAlias implements IFileAliasProvider, IContextProviderExtension, IContextRootProviderExtension,
			IProcessorProviderExtension, IRemoteClientProviderExtension, IServerProviderExtension {

		private String result;
		private Closure<String> aliasClosure;

		public RemoteClient client;
		public ConnectiveHttpServer server;
		public DocumentProcessor documentProcessor;
		public String virtualRoot;
		public FileProviderContext context;

		public ClosureAlias(Closure<String> aliasClosure) {
			this.aliasClosure = aliasClosure;
			aliasClosure.setDelegate(this);
		}

		@Override
		@SuppressWarnings("unchecked")
		public IFileAliasProvider createInstance() {
			return new ClosureAlias((Closure<String>) aliasClosure.clone());
		}

		@Override
		public boolean match(HttpRequest request, String inputPath) {
			if (result != null)
				return true;
			result = aliasClosure.call(request, inputPath);
			return result != null;
		}

		@Override
		public String applyAlias(HttpRequest request, String inputPath) {
			if (result == null)
				if (!match(request, inputPath))
					return null;
			return result;
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

	}

	/**
	 * Adds aliases
	 * 
	 * @param alias Alias to add
	 */
	public void Alias(IFileAliasProvider alias) {
		aliases.add(alias);
	}

	/**
	 * Adds aliases
	 * 
	 * @param aliasClassName Alias class name
	 * @param initParams     Constructor parameters
	 */
	public void Alias(String aliasClassName, Object... initParams) {
		try {
			Class<?> cls = Class.forName(aliasClassName, true, ConnectiveStandaloneMain.getModuleClassLoader());
			if (IFileAliasProvider.class.isAssignableFrom(cls)) {
				try {
					Alias((IFileAliasProvider) cls
							.getConstructor(Stream.of(initParams).map(t -> t.getClass()).toArray(t -> new Class[t]))
							.newInstance(initParams));
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new IllegalArgumentException("Invalid alias class name: " + aliasClassName
							+ ": no constructor matching the passed arguments");
				}
			} else
				throw new IllegalArgumentException(
						"Invalid alias class name: " + aliasClassName + ": not a alias type");
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Invalid alias class name: " + aliasClassName);
		}
	}

	/**
	 * Adds aliases
	 * 
	 * @param aliasClosure Alias closure to add
	 */
	public void Alias(Closure<String> aliasClosure) {
		Alias(new ClosureAlias(aliasClosure));
	}

	public static AliasConfig fromClosure(Closure<?> closure) {
		AliasConfig conf = new AliasConfig();
		closure.setDelegate(conf);
		closure.call();
		return conf;
	}

}
