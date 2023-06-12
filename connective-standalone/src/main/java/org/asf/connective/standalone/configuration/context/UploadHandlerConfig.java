package org.asf.connective.standalone.configuration.context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.basicfile.DocumentProcessor;
import org.asf.connective.basicfile.FileProviderContext;
import org.asf.connective.basicfile.providers.FileUploadHandlerProvider;
import org.asf.connective.basicfile.providers.extensions.IContextProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IContextRootProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IProcessorProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IRemoteClientProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IServerProviderExtension;
import org.asf.connective.standalone.ConnectiveStandaloneMain;
import org.asf.connective.objects.HttpRequest;

import groovy.lang.Closure;

public class UploadHandlerConfig {

	public ArrayList<FileUploadHandlerProvider> uploadHandlers = new ArrayList<FileUploadHandlerProvider>();

	public class ClosureUploadHandler extends FileUploadHandlerProvider
			implements IContextProviderExtension, IContextRootProviderExtension, IProcessorProviderExtension,
			IRemoteClientProviderExtension, IServerProviderExtension {

		private Closure<?> uploadHandlerClosure;
		private Closure<Boolean> matchClosure;
		private boolean supportsDirectories;
		private String path;

		public RemoteClient client;
		public ConnectiveHttpServer server;
		public DocumentProcessor documentProcessor;
		public String virtualRoot;
		public FileProviderContext context;

		public ClosureUploadHandler(Closure<?> uploadHandlerClosure, Closure<Boolean> matchClosure,
				boolean supportsDirectories, String path) {
			this.matchClosure = matchClosure;
			this.uploadHandlerClosure = uploadHandlerClosure;
			this.supportsDirectories = supportsDirectories;
			this.path = path;
			uploadHandlerClosure.setDelegate(this);
			if (matchClosure != null)
				matchClosure.setDelegate(this);
		}

		@Override
		@SuppressWarnings("unchecked")
		protected FileUploadHandlerProvider createInstance() {
			return new ClosureUploadHandler((Closure<?>) uploadHandlerClosure.clone(),
					matchClosure != null ? ((Closure<Boolean>) matchClosure.clone()) : null, supportsDirectories, path);
		}

		@Override
		public boolean match(HttpRequest request, String inputPath, String method) {
			if (matchClosure == null)
				return path.equalsIgnoreCase(inputPath) || path.equals("/")
						|| inputPath.toLowerCase().startsWith(path.toLowerCase() + "/");
			return matchClosure.call(request, inputPath, method);
		}

		@Override
		public void process(File file, String path, String method, RemoteClient client, String contentType)
				throws IOException {
			uploadHandlerClosure.call(file, path, method, client, contentType);
		}

		@Override
		public boolean supportsDirectories() {
			return supportsDirectories;
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

	public class SimpleFileUploadHandler extends FileUploadHandlerProvider
			implements IContextProviderExtension, IContextRootProviderExtension, IProcessorProviderExtension,
			IRemoteClientProviderExtension, IServerProviderExtension {

		private Closure<Boolean> securityCheckClosure;
		private Closure<Boolean> matchClosure;
		private boolean supportsDirectories;
		private String path;

		public RemoteClient client;
		public ConnectiveHttpServer server;
		public DocumentProcessor documentProcessor;
		public String virtualRoot;
		public FileProviderContext context;

		public SimpleFileUploadHandler(Closure<Boolean> securityCheckClosure, Closure<Boolean> matchClosure,
				boolean supportsDirectories, String path) {
			this.matchClosure = matchClosure;
			this.securityCheckClosure = securityCheckClosure;
			this.supportsDirectories = supportsDirectories;
			this.path = path;
			if (securityCheckClosure != null)
				securityCheckClosure.setDelegate(this);
			if (matchClosure != null)
				matchClosure.setDelegate(this);
		}

		@Override
		@SuppressWarnings("unchecked")
		protected FileUploadHandlerProvider createInstance() {
			return new SimpleFileUploadHandler(
					securityCheckClosure != null ? (Closure<Boolean>) securityCheckClosure.clone() : null,
					matchClosure != null ? ((Closure<Boolean>) matchClosure.clone()) : null, supportsDirectories, path);
		}

		@Override
		public boolean match(HttpRequest request, String inputPath, String method) {
			if (matchClosure == null)
				return (method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("DELETE"))
						&& (path.equalsIgnoreCase(inputPath) || path.equals("/")
								|| inputPath.toLowerCase().startsWith(path.toLowerCase() + "/"));
			return matchClosure.call(request, inputPath, method);
		}

		@Override
		public void process(File file, String path, String method, RemoteClient client, String contentType)
				throws IOException {
			// Check security
			if (securityCheckClosure != null) {
				int oldStatus = getResponse().getResponseCode();
				if (!securityCheckClosure.call(getRequest(), path, method)) {
					// Set status to 403 if not changed yet
					if (getResponse().getResponseCode() == oldStatus)
						this.setResponseStatus(403, "Forbidden");
					return;
				}
			}

			// Check validity
			if (file.isDirectory() && !method.equalsIgnoreCase("DELETE")) {
				this.setResponseStatus(400, "Bad request");
				return;
			}

			// Check method
			if (method.equalsIgnoreCase("DELETE")) {
				// Check if its a directory
				if (file.isDirectory()) {
					// Delete directory
					deleteDir(file);
					setResponseStatus(200, "OK");
				} else {
					// Delete file
					file.delete();
					setResponseStatus(200, "OK");
				}
			} else {
				// Create/update file
				if (file.isDirectory()) {
					// Error
					this.setResponseStatus(400, "Bad request");
					return;
				}

				// Check body
				if (!getRequest().hasRequestBody()) {
					// Error
					this.setResponseStatus(400, "Bad request");
					return;
				}

				// Create parent file
				file.getParentFile().mkdirs();

				// Check if it exists
				boolean existed = file.exists();

				// Update/create
				FileOutputStream fOut = new FileOutputStream(file);
				getRequest().transferRequestBody(fOut);
				fOut.close();

				// Set status
				if (!existed)
					this.setResponseStatus(201, "Created");
				else
					this.setResponseStatus(200, "OK");
			}
		}

		private void deleteDir(File dir) {
			for (File f : dir.listFiles(t -> !t.isFile()))
				deleteDir(f);
			for (File f : dir.listFiles(t -> t.isFile()))
				f.delete();
			dir.delete();
		}

		@Override
		public boolean supportsDirectories() {
			return supportsDirectories;
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
	 * Adds upload handler
	 * 
	 * @param uploadHandler Upload handler to add
	 */
	public void UploadHandler(FileUploadHandlerProvider uploadHandler) {
		uploadHandlers.add(uploadHandler);
	}

	/**
	 * Adds upload handler
	 * 
	 * @param uploadHandlerClassName Upload handler class name
	 * @param initParams             Constructor parameters
	 */
	public void UploadHandler(String uploadHandlerClassName, Object... initParams) {
		try {
			Class<?> cls = Class.forName(uploadHandlerClassName, true, ConnectiveStandaloneMain.getModuleClassLoader());
			if (FileUploadHandlerProvider.class.isAssignableFrom(cls)) {
				try {
					UploadHandler((FileUploadHandlerProvider) cls
							.getConstructor(Stream.of(initParams).map(t -> t.getClass()).toArray(t -> new Class[t]))
							.newInstance(initParams));
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new IllegalArgumentException("Invalid upload handler class name: " + uploadHandlerClassName
							+ ": no constructor matching the passed arguments");
				}
			} else
				throw new IllegalArgumentException(
						"Invalid upload handler class name: " + uploadHandlerClassName + ": not a uploadHandler type");
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Invalid upload handler class name: " + uploadHandlerClassName);
		}
	}

	/**
	 * Adds upload handler
	 * 
	 * @param path                 Path string
	 * @param uploadHandlerClosure Upload handler closure to add
	 */
	public void UploadHandler(String path, Closure<?> uploadHandlerClosure) {
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
		UploadHandler(new ClosureUploadHandler(uploadHandlerClosure, null, false, path));
	}

	/**
	 * Adds upload handler
	 * 
	 * @param path                 Path string
	 * @param uploadHandlerClosure Upload handler closure to add
	 * @param supportsDirectories  True to support directories, false otherwise
	 */
	public void UploadHandler(String path, Closure<?> uploadHandlerClosure, boolean supportsDirectories) {
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
		UploadHandler(new ClosureUploadHandler(uploadHandlerClosure, null, supportsDirectories, path));
	}

	/**
	 * Adds upload handler
	 * 
	 * @param matchClosure         Closure called to check if the request matches
	 *                             the upload handler
	 * @param uploadHandlerClosure Upload handler closure to add
	 */
	public void UploadHandler(Closure<Boolean> matchClosure, Closure<?> uploadHandlerClosure) {
		UploadHandler(new ClosureUploadHandler(uploadHandlerClosure, matchClosure, false, null));
	}

	/**
	 * Adds upload handler
	 * 
	 * @param matchClosure         Closure called to check if the request matches
	 *                             the upload handler
	 * @param uploadHandlerClosure Upload handler closure to add
	 * @param supportsDirectories  True to support directories, false otherwise
	 */
	public void UploadHandler(Closure<Boolean> matchClosure, Closure<?> uploadHandlerClosure,
			boolean supportsDirectories) {
		UploadHandler(new ClosureUploadHandler(uploadHandlerClosure, matchClosure, supportsDirectories, null));
	}

	/**
	 * Creates a default upload handler (NO SECURITY)
	 * 
	 * @param path Path string
	 * @return FileUploadHandlerProvider instance
	 */
	public FileUploadHandlerProvider FileUpload(String path) {
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
		return new SimpleFileUploadHandler(null, null, false, path);
	}

	/**
	 * Creates a default upload handler (NO SECURITY)
	 * 
	 * @param path                Path string
	 * @param supportsDirectories True to support directories, false otherwise
	 * @return FileUploadHandlerProvider instance
	 */
	public FileUploadHandlerProvider FileUpload(String path, boolean supportsDirectories) {
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
		return new SimpleFileUploadHandler(null, null, supportsDirectories, path);
	}

	/**
	 * Creates a default upload handler
	 * 
	 * @param path                Path string
	 * @param supportsDirectories True to support directories, false otherwise
	 * @param securityClosure     Security closure to call to verify the request
	 * @return FileUploadHandlerProvider instance
	 */
	public FileUploadHandlerProvider FileUpload(String path, boolean supportsDirectories,
			Closure<Boolean> securityClosure) {
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
		return new SimpleFileUploadHandler(securityClosure, null, supportsDirectories, path);
	}

	/**
	 * Creates a default upload handler
	 * 
	 * @param path            Path string
	 * @param securityClosure Security closure to call to verify the request
	 * @return FileUploadHandlerProvider instance
	 */
	public FileUploadHandlerProvider FileUpload(String path, Closure<Boolean> securityClosure) {
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
		return new SimpleFileUploadHandler(securityClosure, null, false, path);
	}

	/**
	 * Creates a default upload handler (NO SECURITY)
	 * 
	 * @param matchClosure Closure called to check if the request matches the upload
	 *                     handler
	 */
	public FileUploadHandlerProvider FileUpload(Closure<Boolean> matchClosure) {
		return new SimpleFileUploadHandler(null, matchClosure, false, null);
	}

	/**
	 * Creates a default upload handler (NO SECURITY)
	 * 
	 * @param matchClosure        Closure called to check if the request matches the
	 *                            upload handler
	 * @param supportsDirectories True to support directories, false otherwise
	 * @return FileUploadHandlerProvider instance
	 */
	public FileUploadHandlerProvider FileUpload(Closure<Boolean> matchClosure, boolean supportsDirectories) {
		return new SimpleFileUploadHandler(null, matchClosure, supportsDirectories, null);
	}

	/**
	 * Creates a default upload handler
	 * 
	 * @param matchClosure    Closure called to check if the request matches the
	 *                        upload handler
	 * @param securityClosure Security closure to call to verify the request
	 * @return FileUploadHandlerProvider instance
	 */
	public FileUploadHandlerProvider FileUpload(Closure<Boolean> matchClosure, Closure<Boolean> securityClosure) {
		return new SimpleFileUploadHandler(securityClosure, matchClosure, false, null);
	}

	/**
	 * Creates a default upload handler
	 * 
	 * @param matchClosure        Closure called to check if the request matches the
	 *                            upload handler
	 * @param supportsDirectories True to support directories, false otherwise
	 * @param securityClosure     Security closure to call to verify the request
	 * @return FileUploadHandlerProvider instance
	 */
	public FileUploadHandlerProvider FileUpload(Closure<Boolean> matchClosure, boolean supportsDirectories,
			Closure<Boolean> securityClosure) {
		return new SimpleFileUploadHandler(securityClosure, matchClosure, supportsDirectories, null);
	}

	public static UploadHandlerConfig fromClosure(Closure<?> closure) {
		UploadHandlerConfig conf = new UploadHandlerConfig();
		closure.setDelegate(conf);
		closure.call();
		return conf;
	}

}
