package org.asf.connective.basicfile;

import java.util.ArrayList;
import java.util.HashMap;

import org.asf.connective.ContentSource;
import org.asf.connective.HandlerSetContentSource;
import org.asf.connective.basicfile.impl.DefaultIndexPage;
import org.asf.connective.basicfile.providers.FileUploadHandlerProvider;
import org.asf.connective.basicfile.providers.IDocumentPostProcessorProvider;
import org.asf.connective.basicfile.providers.IFileAliasProvider;
import org.asf.connective.basicfile.providers.IFileExtensionProvider;
import org.asf.connective.basicfile.providers.IFileRestrictionProvider;
import org.asf.connective.basicfile.providers.IVirtualFileProvider;
import org.asf.connective.basicfile.providers.IndexPageProvider;
import org.asf.connective.handlers.DynamicHttpPushHandler;
import org.asf.connective.handlers.DynamicHttpRequestHandler;
import org.asf.connective.handlers.HttpHandlerSet;
import org.asf.connective.handlers.HttpPushHandler;
import org.asf.connective.handlers.HttpRequestHandler;

/**
 * 
 * File provider context factory - used to create {@link FileProviderContext}
 * instances.
 * 
 * @author Sky Swimmer
 *
 */
public class FileProviderContextFactory {
	protected ArrayList<IFileAliasProvider> aliases = new ArrayList<IFileAliasProvider>();
	protected ArrayList<FileUploadHandlerProvider> uploadHandlers = new ArrayList<FileUploadHandlerProvider>();
	protected ArrayList<IDocumentPostProcessorProvider> postProcessors = new ArrayList<IDocumentPostProcessorProvider>();
	protected ArrayList<IFileExtensionProvider> extensions = new ArrayList<IFileExtensionProvider>();
	protected ArrayList<IFileRestrictionProvider> restrictions = new ArrayList<IFileRestrictionProvider>();
	protected HashMap<String, IndexPageProvider> indexPages = new HashMap<String, IndexPageProvider>();
	protected ArrayList<IVirtualFileProvider> virtualFiles = new ArrayList<IVirtualFileProvider>();
	protected HttpHandlerSet processors = new HttpHandlerSet();
	protected ContentSource source = new HandlerSetContentSource(processors);
	protected IndexPageProvider defaultIndexPage = new DefaultIndexPage();
	protected String fileSourceFolder = "root";

	/**
	 * Assigns the source folder used to retrieve files for the HTTP server, this is
	 * the physical webroot folder
	 * 
	 * @param path Source folder path
	 */
	public void setFileSourceFolder(String path) {
		fileSourceFolder = path;
	}

	/**
	 * Assigns the ContentSource instance used by the server
	 * 
	 * @param newSource New ContentSource instance to handle server requests, sets
	 *                  the old content source as parent to the new content source
	 * @since Connective 1.0.0.A17
	 */
	public void setContentSource(ContentSource newSource) {
		newSource.unsafe().unsafeSetParent(newSource);
		source = newSource;
	}

	/**
	 * Registers a new push processor
	 * 
	 * @param processor The processor implementation to register
	 */
	public void registerProcessor(DynamicHttpPushHandler processor) {
		processors.registerHandler(processor);
	}

	/**
	 * Registers a new request processor
	 * 
	 * @param processor The processor implementation to register.
	 */
	public void registerProcessor(DynamicHttpRequestHandler processor) {
		processors.registerHandler(processor);
	}

	/**
	 * Registers a new push processor
	 * 
	 * @param processor The processor implementation to register
	 */
	public void registerProcessor(HttpPushHandler processor) {
		processors.registerHandler(processor);
	}

	/**
	 * Registers a new request processor
	 * 
	 * @param processor The processor implementation to register.
	 */
	public void registerProcessor(HttpRequestHandler processor) {
		processors.registerHandler(processor);
	}

	/**
	 * Registers file alias providers
	 * 
	 * @param alias Alias provider to register
	 */
	public void registerAlias(IFileAliasProvider alias) {
		aliases.add(alias);
	}

	/**
	 * Registers file upload handler providers
	 * 
	 * @param handler Upload handler provider to register
	 */
	public void registerUploadHandler(FileUploadHandlerProvider handler) {
		uploadHandlers.add(handler);
	}

	/**
	 * Registers file post-processor providers
	 * 
	 * @param postProcessor Post-processor to register
	 */
	public void registerPostProcessor(IDocumentPostProcessorProvider postProcessor) {
		postProcessors.add(postProcessor);
	}

	/**
	 * Registers file extension providers
	 * 
	 * @param fileExtension File extension provider to register
	 */
	public void registerFileExtension(IFileExtensionProvider fileExtension) {
		extensions.add(fileExtension);
	}

	/**
	 * Registers restriction providers
	 * 
	 * @param restriction Restriction provider to register
	 */
	public void registerRestriction(IFileRestrictionProvider restriction) {
		restrictions.add(restriction);
	}

	/**
	 * Registers index pages
	 * 
	 * @param path      Folder path
	 * @param indexPage Index page to register
	 */
	public void registerIndexPage(String path, IndexPageProvider indexPage) {
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

		// Add index page
		indexPages.put(path, indexPage);
	}

	/**
	 * Assigns the default index page
	 * 
	 * @param indexPage IndexPageProvider instance to set as default index page
	 */
	public void setDefaultIndexPage(IndexPageProvider indexPage) {
		defaultIndexPage = indexPage;
	}

	/**
	 * Registers virtual file providers
	 * 
	 * @param virtualFile Virtual file provider to register
	 */
	public void registerVirtualFile(IVirtualFileProvider virtualFile) {
		virtualFiles.add(virtualFile);
	}

	/**
	 * Builds the FileProviderContext instance
	 * 
	 * @return FileProviderContext instance
	 */
	public FileProviderContext build() {
		FileProviderContext ctx = new FileProviderContext();
		ctx.fileSourceFolder = fileSourceFolder;
		ctx.defaultIndexPage = defaultIndexPage;
		ctx.source = source;
		ctx.processors = processors;
		ctx.uploadHandlers.addAll(uploadHandlers);
		ctx.postProcessors.addAll(postProcessors);
		ctx.restrictions.addAll(restrictions);
		ctx.extensions.addAll(extensions);
		ctx.indexPages.putAll(indexPages);
		ctx.aliases.addAll(aliases);
		ctx.virtualFiles.addAll(virtualFiles);
		return ctx;
	}

}
