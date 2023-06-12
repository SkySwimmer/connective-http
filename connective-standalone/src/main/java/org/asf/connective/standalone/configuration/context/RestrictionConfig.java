package org.asf.connective.standalone.configuration.context;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.basicfile.DocumentProcessor;
import org.asf.connective.basicfile.FileProviderContext;
import org.asf.connective.basicfile.providers.IFileRestrictionProvider;
import org.asf.connective.basicfile.providers.extensions.IContextProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IContextRootProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IProcessorProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IRemoteClientProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IServerProviderExtension;
import org.asf.connective.standalone.ConnectiveStandaloneMain;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

import groovy.lang.Closure;

public class RestrictionConfig {

	public ArrayList<IFileRestrictionProvider> restrictions = new ArrayList<IFileRestrictionProvider>();

	public class ClosureRestriction
			implements IFileRestrictionProvider, IContextProviderExtension, IContextRootProviderExtension,
			IProcessorProviderExtension, IRemoteClientProviderExtension, IServerProviderExtension {

		private Closure<Boolean> restrictionClosure;
		private Closure<Boolean> matchClosure;
		private String path;

		public RemoteClient client;
		public ConnectiveHttpServer server;
		public DocumentProcessor documentProcessor;
		public String virtualRoot;
		public FileProviderContext context;

		public ClosureRestriction(Closure<Boolean> restrictionClosure, Closure<Boolean> matchClosure, String path) {
			this.matchClosure = matchClosure;
			this.restrictionClosure = restrictionClosure;
			this.path = path;
			restrictionClosure.setDelegate(this);
			if (matchClosure != null)
				matchClosure.setDelegate(this);
		}

		@Override
		@SuppressWarnings("unchecked")
		public IFileRestrictionProvider createInstance() {
			return new ClosureRestriction((Closure<Boolean>) restrictionClosure.clone(),
					matchClosure != null ? ((Closure<Boolean>) matchClosure.clone()) : null, path);
		}

		@Override
		public boolean match(HttpRequest request, String inputPath) {
			if (matchClosure == null)
				return path.equalsIgnoreCase(inputPath) || path.equals("/")
						|| inputPath.toLowerCase().startsWith(path.toLowerCase() + "/");
			return matchClosure.call(request, inputPath);
		}

		@Override
		public boolean checkRestriction(String file, HttpRequest request, HttpResponse response) {
			return restrictionClosure.call(file, request, response);
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
	 * Adds restrictions
	 * 
	 * @param restriction Restriction to add
	 */
	public void Restriction(IFileRestrictionProvider restriction) {
		restrictions.add(restriction);
	}

	/**
	 * Adds restriction
	 * 
	 * @param restrictionClassName Restriction class name
	 * @param initParams           Constructor parameters
	 */
	public void Restriction(String restrictionClassName, Object... initParams) {
		try {
			Class<?> cls = Class.forName(restrictionClassName, true, ConnectiveStandaloneMain.getModuleClassLoader());
			if (IFileRestrictionProvider.class.isAssignableFrom(cls)) {
				try {
					Restriction((IFileRestrictionProvider) cls
							.getConstructor(Stream.of(initParams).map(t -> t.getClass()).toArray(t -> new Class[t]))
							.newInstance(initParams));
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new IllegalArgumentException("Invalid restriction class name: " + restrictionClassName
							+ ": no constructor matching the passed arguments");
				}
			} else
				throw new IllegalArgumentException(
						"Invalid restriction class name: " + restrictionClassName + ": not a restriction type");
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Invalid restriction class name: " + restrictionClassName);
		}
	}

	/**
	 * Adds restriction
	 * 
	 * @param path               Path string
	 * @param restrictionClosure Restriction closure to add
	 */
	public void Restriction(String path, Closure<Boolean> restrictionClosure) {
		while (path.startsWith("/"))
			path = path.substring(1);
		while (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		while (path.contains("//"))
			path = path.replace("//", "/");
		if (path.contains("\\"))
			path = path.replace("\\", "/");
		if (!path.startsWith("/"))
			path = "/" + path;
		Restriction(new ClosureRestriction(restrictionClosure, null, path));
	}

	/**
	 * Adds restriction
	 * 
	 * @param matchClosure       Closure called to check if the request matches the
	 *                           restriction
	 * @param restrictionClosure Restriction closure to add
	 */
	public void Restriction(Closure<Boolean> matchClosure, Closure<Boolean> restrictionClosure) {
		Restriction(new ClosureRestriction(restrictionClosure, matchClosure, null));
	}

	public static RestrictionConfig fromClosure(Closure<?> closure) {
		RestrictionConfig conf = new RestrictionConfig();
		closure.setDelegate(conf);
		closure.call();
		return conf;
	}

}
