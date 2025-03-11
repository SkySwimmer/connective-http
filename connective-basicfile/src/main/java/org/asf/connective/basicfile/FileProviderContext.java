package org.asf.connective.basicfile;

import java.util.ArrayList;
import java.util.HashMap;

import org.asf.connective.processors.HttpPushProcessor;
import org.asf.connective.processors.HttpRequestProcessor;

import org.asf.connective.basicfile.providers.IFileAliasProvider;
import org.asf.connective.basicfile.providers.FileUploadHandlerProvider;
import org.asf.connective.basicfile.providers.IFileExtensionProvider;
import org.asf.connective.basicfile.providers.IDocumentPostProcessorProvider;
import org.asf.connective.basicfile.providers.IFileRestrictionProvider;
import org.asf.connective.basicfile.providers.IVirtualFileProvider;
import org.asf.connective.basicfile.providers.IndexPageProvider;

/**
 * 
 * File context - contains response information for the request, use
 * {@link FileProviderContextFactory} to create one.
 * 
 * @author Sky Swimmer
 *
 */
public class FileProviderContext {
	protected ArrayList<IFileAliasProvider> aliases = new ArrayList<IFileAliasProvider>();
	protected ArrayList<FileUploadHandlerProvider> uploadHandlers = new ArrayList<FileUploadHandlerProvider>();
	protected ArrayList<IDocumentPostProcessorProvider> postProcessors = new ArrayList<IDocumentPostProcessorProvider>();
	protected ArrayList<IFileExtensionProvider> extensions = new ArrayList<IFileExtensionProvider>();
	protected ArrayList<IFileRestrictionProvider> restrictions = new ArrayList<IFileRestrictionProvider>();
	protected HashMap<String, IndexPageProvider> indexPages = new HashMap<String, IndexPageProvider>();
	protected ArrayList<IVirtualFileProvider> virtualFiles = new ArrayList<IVirtualFileProvider>();
	protected ArrayList<HttpRequestProcessor> requestProcessors = new ArrayList<HttpRequestProcessor>();
	protected ArrayList<HttpPushProcessor> pushProcessors = new ArrayList<HttpPushProcessor>();
	protected IndexPageProvider defaultIndexPage = null;
	protected String fileSourceFolder;

	/**
	 * Retrieves the path of the webroot folder
	 * 
	 * @return Webroot folder path
	 */
	public String getWebrootFolderPath() {
		return fileSourceFolder;
	}

	/**
	 * Retrieves the HTTP push processors registered in this context
	 * 
	 * @return Array of HttpPushProcessor instances
	 */
	public HttpPushProcessor[] getPushProcessors() {
		return pushProcessors.toArray(new HttpPushProcessor[0]);
	}

	/**
	 * Retrieves the HTTP request processors registered in this context
	 * 
	 * @return Array of HttpRequestProcessor instances
	 */
	public HttpRequestProcessor[] getRequestProcessors() {
		return requestProcessors.toArray(new HttpRequestProcessor[0]);
	}

	/**
	 * Retrieves the file aliases registered in this context
	 * 
	 * @return Array of IFileAliasProvider instances
	 */
	public IFileAliasProvider[] getAliases() {
		return aliases.toArray(new IFileAliasProvider[0]);
	}

	/**
	 * Retrieves the file upload handlers registered in this context
	 * 
	 * @return Array of FileUploadHandlerProvider instances
	 */
	public FileUploadHandlerProvider[] getUploadHandlers() {
		return uploadHandlers.toArray(new FileUploadHandlerProvider[0]);
	}

	/**
	 * Retrieves the file post-processors registered in this context
	 * 
	 * @return Array of IDocumentPostProcessorProvider instances
	 */
	public IDocumentPostProcessorProvider[] getPostProcessors() {
		return postProcessors.toArray(new IDocumentPostProcessorProvider[0]);
	}

	/**
	 * Retrieves the file extension processors registered in this context
	 * 
	 * @return Array of IFileExtensionProvider instances
	 */
	public IFileExtensionProvider[] getFileExtensions() {
		return extensions.toArray(new IFileExtensionProvider[0]);
	}

	/**
	 * Retrieves the restriction providers registered in this context
	 * 
	 * @return Array of IFileRestrictionProvider instances
	 */
	public IFileRestrictionProvider[] getRestrictions() {
		return restrictions.toArray(new IFileRestrictionProvider[0]);
	}

	/**
	 * Retrieves the virtual files registered in this context
	 * 
	 * @return Array of IVirtualFileProvider instances
	 */
	public IVirtualFileProvider[] getVirtualFiles() {
		return virtualFiles.toArray(new IVirtualFileProvider[0]);
	}

	/**
	 * Retrieves an index page provider for the given path (does not instantiate it)
	 * 
	 * @param path Path to retrieve the index page for
	 * @return IndexPageProvider instance or null
	 */
	public IndexPageProvider getIndexPage(String path) {
		// Clean path
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

		// Find best index page for this page
		for (String pth : indexPages.keySet().stream().sorted((t1, t2) -> {
			return -Integer.compare(t1.split("/").length, t2.split("/").length);
		}).toArray(t -> new String[t])) {
			// Check if its exactly this path
			if (pth.equals("/") || pth.equalsIgnoreCase(path)) {
				return indexPages.get(pth);
			} else if (path.toLowerCase().startsWith(pth.toLowerCase() + "/")) {
				// Less specific but still a match
				return indexPages.get(pth);
			}
		}

		// Return default instead
		return defaultIndexPage;
	}

}
