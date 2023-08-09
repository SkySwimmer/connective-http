package org.asf.connective.basicfile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.stream.Stream;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.basicfile.providers.FileUploadHandlerProvider;
import org.asf.connective.basicfile.providers.IDocumentPostProcessorProvider;
import org.asf.connective.basicfile.providers.IFileAliasProvider;
import org.asf.connective.basicfile.providers.IFileExtensionProvider;
import org.asf.connective.basicfile.providers.IFileRestrictionProvider;
import org.asf.connective.basicfile.providers.IVirtualFileProvider;
import org.asf.connective.basicfile.providers.IndexPageProvider;
import org.asf.connective.basicfile.providers.extensions.IContextProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IContextRootProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IProcessorProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IRemoteClientProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IServerProviderExtension;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.connective.processors.HttpRequestProcessor;

/**
 * 
 * Document Processor - called to process requests and to locate files related
 * to requests
 * 
 * @author Sky Swimmer
 *
 */
public class DocumentProcessor {
	protected FileProviderContext context;
	protected String virtualRoot;

	public DocumentProcessor(FileProviderContext context, String virtualRoot) {
		// Clean path
		virtualRoot = sanitizePath(virtualRoot);

		// Assign fields
		this.context = context;
		this.virtualRoot = virtualRoot;
	}

	private String sanitizePath(String path) {
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
		return path;
	}

	/**
	 * Retrieves the virtual root for this processor
	 * 
	 * @return Virtual root string
	 */
	public String getVirtualRoot() {
		return virtualRoot;
	}

	/**
	 * Processes HTTP requests
	 * 
	 * @param path     Path relative to the virtual root
	 * @param request  Request instance
	 * @param response Response instance
	 * @param client   Client making the request
	 * @return True if handled, false otherwise
	 * @throws IOException If processing fails
	 */
	public boolean processRequest(String path, HttpRequest request, HttpResponse response, RemoteClient client)
			throws IOException {
		path = sanitizePath(path);
		ConnectiveHttpServer server = client.getServer();

		// First, aliases
		for (IFileAliasProvider alias : context.getAliases()) {
			// Create instance
			alias = alias.createInstance();

			// Provide information
			provideDataTo(alias, client, server);

			// Verify if its able to handle the request
			if (alias.match(request, path)) {
				// Apply alias
				path = sanitizePath(alias.applyAlias(request, path));

				// Make sure its not attempting to access a resource outside of the scope
				if (path.startsWith("..") || path.endsWith("..") || path.contains("/..") || path.contains("../")) {
					response.setResponseStatus(403, "Forbidden");
					response.setContent("text/html", server.getErrorPageGenerator().apply(response, request));

					// Post-process
					postProcessRequest(path, request, response, client, server);
					return true;
				}
			}
		}

		// Restrictions
		for (IFileRestrictionProvider restriction : context.getRestrictions()) {
			// Create instance
			restriction = restriction.createInstance();

			// Provide information
			provideDataTo(restriction, client, server);

			// Verify
			if (restriction.match(request, path)) {
				// Check restriction
				int oldStatus = response.getResponseCode();
				if (!restriction.checkRestriction(path, request, response)) {
					// Content is restricted

					// Set result
					restriction.rewriteResponse(request, response);
					if (oldStatus == response.getResponseCode()) {
						response.setResponseStatus(restriction.getResponseCode(request),
								restriction.getResponseMessage(request));
					}
					if (!response.hasResponseBody())
						response.setContent("text/html", server.getErrorPageGenerator().apply(response, request));

					// Post-process
					postProcessRequest(path, request, response, client, server);
					return true;
				}
			}
		}

		// Request processing
		if (handleRequestProcessors(path, request, response, client, server))
			return true;

		// Virtual files
		for (IVirtualFileProvider prov : context.getVirtualFiles()) {
			// Create instance
			prov = prov.createInstance();

			// Provide information
			provideDataTo(prov, client, server);

			// Verify
			if (prov.match(request, path)) {
				// Found it

				// Run processor
				if (!request.hasRequestBody() && !request.getRequestMethod().equalsIgnoreCase("DELETE")
						&& !request.getRequestMethod().equalsIgnoreCase("PUT")
						&& !request.getRequestMethod().equalsIgnoreCase("POST")) {
					// Run processor
					response.setResponseStatus(200, "OK");
					prov.process(request.getRequestMethod(), request, response, path, null, client);

					// Post-process
					postProcessRequest(path, request, response, client, server);
				} else {
					// Check
					if (prov.supportsPush()) {
						// Run processor
						response.setResponseStatus(200, "OK");
						prov.process(request.getRequestMethod(), request, response, path,
								request.getHeaderValue("Content-Type"), client);

						// Post-process
						postProcessRequest(path, request, response, client, server);
					} else {
						response.setResponseStatus(403, "Forbidden");
						response.setContent("text/html", server.getErrorPageGenerator().apply(response, request));
						postProcessRequest(path, request, response, client, server);
					}
				}

				// Return finish
				return true;
			}
		}

		// Find file
		File sourceFile = new File(context.getWebrootFolderPath(), path);
		if (!sourceFile.exists() && !request.getRequestMethod().equals("POST")
				&& !request.getRequestMethod().equals("PUT")) {
			// File not found
			return false;
		}

		// Check if its a directory
		if (sourceFile.isDirectory() && !request.getRequestMethod().equals("DELETE")
				&& !request.getRequestMethod().equals("POST") && !request.getRequestMethod().equals("PUT")) {
			// Find index page

			// Find one by extension
			File indexPage = null;
			for (IFileExtensionProvider prov : context.getFileExtensions()) {
				// Check
				File potentialIndex = new File(sourceFile, "index" + prov.fileExtension());
				if (potentialIndex.exists()) {
					indexPage = potentialIndex;
					break;
				}
			}
			if (indexPage == null) {
				// Find in directory
				File[] files = sourceFile.listFiles(t -> !t.isDirectory() && t.getName().startsWith("index.")
						&& !t.getName().substring("index.".length()).contains("."));
				if (files.length != 0) {
					if (Stream.of(files).anyMatch(t -> t.getName().equals("index.html")))
						indexPage = new File(sourceFile, "index.html");
					else if (Stream.of(files).anyMatch(t -> t.getName().equals("index.htm")))
						indexPage = new File(sourceFile, "index.htm");
					else
						indexPage = files[0];
				}
			}

			// Check
			if (indexPage != null) {
				// Assign new page
				sourceFile = indexPage;
				path = path + "/" + indexPage.getName();
			} else {
				// Find index page provider
				IndexPageProvider prov = context.getIndexPage(path);
				if (prov == null)
					return false; // Denied listing
				File[] files = Stream.of(sourceFile.listFiles(t -> !t.isDirectory())).sorted()
						.toArray(t -> new File[t]);
				File[] dirs = Stream.of(sourceFile.listFiles(t -> t.isDirectory())).sorted().toArray(t -> new File[t]);
				prov = prov.instantiate(server, request, response, files, dirs, path);

				// Provide info
				provideDataTo(prov, client, server);

				// Run page
				prov.process(path, request.getRequestMethod(), client, files, dirs);

				// Post-process
				postProcessRequest(path, request, response, client, server);
				return true;
			}
		}

		// Check upload
		if (request.getRequestMethod().equals("POST") || request.getRequestMethod().equals("PUT")
				|| request.getRequestMethod().equals("DELETE")) {
			// Upload request
			// Find upload handler
			for (FileUploadHandlerProvider handler : context.getUploadHandlers()) {
				// Instantiate
				handler = handler.instantiate(server, request, response, path, sourceFile);

				// Provide info
				provideDataTo(handler, client, server);

				// Check match
				if (handler.match(request, path, request.getRequestMethod())) {
					// Found one
					// Check directory support
					if (sourceFile.isDirectory() && !handler.supportsDirectories()) {
						// Return 403 error status
						response.setResponseStatus(403, "Forbidden");
						response.setContent("text/html", server.getErrorPageGenerator().apply(response, request));
						postProcessRequest(path, request, response, client, server);
						return true;
					}

					// Run handler
					handler.process(sourceFile, path, request.getRequestMethod(), client,
							request.getHeaderValue("Content-Type"));

					// Post-process
					postProcessRequest(path, request, response, client, server);
					return true;
				}
			}

			// Unhandled upload request
			// Return 403 error status
			response.setResponseStatus(403, "Forbidden");
			response.setContent("text/html", server.getErrorPageGenerator().apply(response, request));
			postProcessRequest(path, request, response, client, server);
			return true;
		}

		// Open file stream
		FileInputStream strm = new FileInputStream(sourceFile);

		// Set content
		response.setContent(MainFileMap.getInstance().getContentType(sourceFile), strm, sourceFile.length());

		// Find file extensions
		for (IFileExtensionProvider prov : context.getFileExtensions()) {
			// Check file
			if (sourceFile.getName().endsWith(prov.fileExtension())) {
				// Instantiate
				prov = prov.createInstance();

				// Provide info
				provideDataTo(prov, client, server);

				// Apply extension preprocessor
				response = prov.rewrite(path, response.getBodyStream(), response, request).getRewrittenResponse();
				break;
			}
		}

		// Success
		postProcessRequest(path, request, response, client, server);
		return true;
	}

	private static class MainFileMap extends MimetypesFileTypeMap {
		private static MainFileMap instance;

		private FileTypeMap parent;

		public static MainFileMap getInstance() {
			if (instance == null) {
				instance = new MainFileMap(MimetypesFileTypeMap.getDefaultFileTypeMap());
			}
			return instance;
		}

		public MainFileMap(FileTypeMap parent) {
			this.parent = parent;
			this.addMimeTypes("application/xml	xml");
			this.addMimeTypes("application/json	json");
			this.addMimeTypes("text/ini	ini	ini");
			this.addMimeTypes("text/css	css");
			this.addMimeTypes("text/javascript	js");
			if (new File(".mime.types").exists()) {
				try {
					this.addMimeTypes(Files.readString(Path.of(".mime.types")));
				} catch (IOException e) {
				}
			}
			if (new File("mime.types").exists()) {
				try {
					this.addMimeTypes(Files.readString(Path.of("mime.types")));
				} catch (IOException e) {
				}
			}
		}

		@Override
		public String getContentType(String filename) {
			String type = super.getContentType(filename);
			if (type.equals("application/octet-stream")) {
				type = parent.getContentType(filename);
			}
			return type;
		}
	}

	@SuppressWarnings("deprecation")
	private void postProcessRequest(String path, HttpRequest request, HttpResponse response, RemoteClient client,
			ConnectiveHttpServer server) {
		// Set error if needed
		if (!response.isSuccessResponseCode() && !response.hasResponseBody())
			response.setContent("text/html", server.getErrorPageGenerator().apply(response, request));

		// Post process
		for (IDocumentPostProcessorProvider processor : context.getPostProcessors()) {
			// Create instance
			processor = processor.createInstance();

			// Provide information
			provideDataTo(processor, client, server);

			// Verify
			if (processor.match(request, path)) {
				// Run post-processor if possible
				if ((response.hasHeader("Content-Type")
						&& response.getHeaderValue("Content-Type").equalsIgnoreCase("text/html"))
						|| processor.acceptNonHTML()) {
					// Run it
					StringBuilder builder = new StringBuilder();
					processor.setWriteCallback(t -> builder.append(t));
					processor.process(path, request, response, client, request.getRequestMethod());
					byte[] bytes = builder.toString().getBytes();
					ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
					InputStream oldStrm = response.getBodyStream();
					response.body = null;
					response.setContent(new AmendingInputStream(oldStrm, byteStream));
				}
			}
		}
	}

	private boolean handleRequestProcessors(String path, HttpRequest request, HttpResponse response,
			RemoteClient client, ConnectiveHttpServer server) throws IOException {
		// Load handlers
		ArrayList<HttpRequestProcessor> reqProcessors = new ArrayList<HttpRequestProcessor>();
		for (HttpRequestProcessor proc : context.getRequestProcessors())
			reqProcessors.add(proc);
		ArrayList<HttpPushProcessor> pushProcessors = new ArrayList<HttpPushProcessor>();
		for (HttpPushProcessor proc : context.getPushProcessors())
			pushProcessors.add(proc);
		boolean compatible = false;
		for (HttpPushProcessor proc : pushProcessors) {
			if (proc.supportsNonPush()) {
				reqProcessors.add(proc);
			}
		}

		// Find handler
		if (request.hasRequestBody()) {
			HttpPushProcessor impl = null;
			for (HttpPushProcessor proc : pushProcessors) {
				if (!proc.supportsChildPaths()) {
					String url = path;
					if (!url.endsWith("/"))
						url += "/";

					String supportedURL = proc.path();
					if (!supportedURL.endsWith("/"))
						supportedURL += "/";

					if (url.equals(supportedURL)) {
						compatible = true;
						impl = proc;
						break;
					}
				}
			}
			if (!compatible) {
				pushProcessors.sort((t1, t2) -> {
					return -Integer.compare(sanitizePath(t1.path()).split("/").length,
							sanitizePath(t2.path()).split("/").length);
				});
				for (HttpPushProcessor proc : pushProcessors) {
					if (proc.supportsChildPaths()) {
						String url = path;
						if (!url.endsWith("/"))
							url += "/";

						String supportedURL = sanitizePath(proc.path());
						if (!supportedURL.endsWith("/"))
							supportedURL += "/";

						if (url.startsWith(supportedURL)) {
							compatible = true;
							impl = proc;
							break;
						}
					}
				}
			}
			if (compatible) {
				HttpPushProcessor processor = impl.instantiate(server, request, response);
				provideDataTo(processor, client, server);
				processor.process(path, request.getRequestMethod(), client, request.getHeaderValue("Content-Type"));

				// Post-process
				postProcessRequest(path, request, response, client, server);
			}
		} else {
			HttpRequestProcessor impl = null;
			for (HttpRequestProcessor proc : reqProcessors) {
				if (!proc.supportsChildPaths()) {
					String url = path;
					if (!url.endsWith("/"))
						url += "/";

					String supportedURL = proc.path();
					if (!supportedURL.endsWith("/"))
						supportedURL += "/";

					if (url.equals(supportedURL)) {
						compatible = true;
						impl = proc;
						break;
					}
				}
			}
			if (!compatible) {
				reqProcessors.sort((t1, t2) -> {
					return -Integer.compare(sanitizePath(t1.path()).split("/").length,
							sanitizePath(t2.path()).split("/").length);
				});
				for (HttpRequestProcessor proc : reqProcessors) {
					if (proc.supportsChildPaths()) {
						String url = path;
						if (!url.endsWith("/"))
							url += "/";

						String supportedURL = sanitizePath(proc.path());
						if (!supportedURL.endsWith("/"))
							supportedURL += "/";

						if (url.startsWith(supportedURL)) {
							compatible = true;
							impl = proc;
							break;
						}
					}
				}
			}
			if (compatible) {
				HttpRequestProcessor processor = impl.instantiate(server, request, response);
				provideDataTo(processor, client, server);
				processor.process(path, request.getRequestMethod(), client);

				// Post-process
				postProcessRequest(path, request, response, client, server);
			}
		}
		return compatible;
	}

	private void provideDataTo(Object obj, RemoteClient client, ConnectiveHttpServer server) {
		if (obj instanceof IContextProviderExtension)
			((IContextProviderExtension) obj).provide(context);
		if (obj instanceof IContextRootProviderExtension)
			((IContextRootProviderExtension) obj).provideVirtualRoot(virtualRoot);
		if (obj instanceof IProcessorProviderExtension)
			((IProcessorProviderExtension) obj).provide(this);
		if (obj instanceof IRemoteClientProviderExtension)
			((IRemoteClientProviderExtension) obj).provide(client);
		if (obj instanceof IServerProviderExtension)
			((IServerProviderExtension) obj).provide(server);
	}

	private class AmendingInputStream extends InputStream {
		private InputStream delegate;
		private InputStream target;

		public AmendingInputStream(InputStream first, InputStream second) {
			delegate = first;
			target = second;
		}

		@Override
		public int read() throws IOException {
			if (delegate != null)
				try {
					int i = delegate.read();
					if (i != -1) {
						return i;
					}
				} catch (IOException e) {
				}
			if (target != null)
				return target.read();
			else
				return -1;
		}

	}

	/**
	 * Retrieves the file context associated with this processor
	 * 
	 * @return FileProviderContext instance
	 */
	public FileProviderContext getContext() {
		return context;
	}

}
