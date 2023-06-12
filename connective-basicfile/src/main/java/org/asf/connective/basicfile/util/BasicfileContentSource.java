package org.asf.connective.basicfile.util;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.ContentSource;
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
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * Basicfile-based Content Source implementation
 * 
 * @author Sky Swimmer
 *
 */
public class BasicfileContentSource extends ContentSource {

	private LinkedHashMap<String, DocumentProcessor> processors = new LinkedHashMap<String, DocumentProcessor>();

	/**
	 * Registers processing contexts
	 * 
	 * @param virtualRoot Virtual root
	 * @param context     Processing context to register
	 */
	public void registerContext(String virtualRoot, FileProviderContext context) {
		DocumentProcessor processor = new DocumentProcessor(context, virtualRoot);
		processors.put(processor.getVirtualRoot(), processor);
	}

	@Override
	public boolean process(String path, HttpRequest request, HttpResponse response, RemoteClient client,
			ConnectiveHttpServer server) throws IOException {
		// Find virtual root
		for (String pth : processors.keySet().stream().sorted((t1, t2) -> {
			return -Integer.compare(t1.split("/").length, t2.split("/").length);
		}).toArray(t -> new String[t])) {
			// Check path
			if (pth.equals("/") || pth.equalsIgnoreCase(path)
					|| path.toLowerCase().startsWith(pth.toLowerCase() + "/")) {
				// Alias through this virtual root
				DocumentProcessor proc = processors.get(pth);
				for (IFileAliasProvider alias : proc.getContext().getAliases()) {
					// Create instance
					alias = alias.createInstance();

					// Provide information
					provideDataTo(alias, pth, proc.getContext(), proc, client, server);

					// Verify if its able to handle the request
					if (alias.match(request, path)) {
						// Apply alias
						path = sanitizePath(alias.applyAlias(request, path));

						// Make sure its not attempting to access a resource outside of the scope
						if (path.startsWith("..") || path.endsWith("..") || path.contains("/..")
								|| path.contains("../")) {
							response.setResponseStatus(403, "Forbidden");
							return true;
						}
					}
				}
			}
		}

		// Find processor
		for (String pth : processors.keySet().stream().sorted((t1, t2) -> {
			return -Integer.compare(t1.split("/").length, t2.split("/").length);
		}).toArray(t -> new String[t])) {
			// Check path
			if (pth.equals("/") || pth.equalsIgnoreCase(path)
					|| path.toLowerCase().startsWith(pth.toLowerCase() + "/")) {
				// Run processor
				DocumentProcessor proc = processors.get(pth);
				if (proc.processRequest(path.substring(pth.length()), request, response, client))
					return true; // Handled the request
				break;
			}
		}

		// Delegate to parent or fail if none is present
		if (getParent() != null)
			return getParent().process(path, request, response, client, server);
		return false;
	}

	private void provideDataTo(Object obj, String virtualRoot, FileProviderContext context, DocumentProcessor proc,
			RemoteClient client, ConnectiveHttpServer server) {
		if (obj instanceof IContextProviderExtension)
			((IContextProviderExtension) obj).provide(context);
		if (obj instanceof IContextRootProviderExtension)
			((IContextRootProviderExtension) obj).provideVirtualRoot(virtualRoot);
		if (obj instanceof IProcessorProviderExtension)
			((IProcessorProviderExtension) obj).provide(proc);
		if (obj instanceof IRemoteClientProviderExtension)
			((IRemoteClientProviderExtension) obj).provide(client);
		if (obj instanceof IServerProviderExtension)
			((IServerProviderExtension) obj).provide(server);
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
}
