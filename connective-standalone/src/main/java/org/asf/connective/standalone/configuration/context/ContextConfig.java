package org.asf.connective.standalone.configuration.context;

import java.util.ArrayList;

import org.asf.connective.standalone.configuration.HandlerConfig;
import org.asf.connective.basicfile.providers.*;
import org.asf.connective.handlers.HttpRequestHandler;

import groovy.lang.Closure;

public class ContextConfig {

	public String virtualRoot;
	public ArrayList<HttpRequestHandler> handlers = new ArrayList<HttpRequestHandler>();
	public ArrayList<IFileAliasProvider> aliases = new ArrayList<IFileAliasProvider>();
	public ArrayList<FileUploadHandlerProvider> uploadHandlers = new ArrayList<FileUploadHandlerProvider>();
	public ArrayList<IDocumentPostProcessorProvider> postProcessors = new ArrayList<IDocumentPostProcessorProvider>();
	public ArrayList<IFileRestrictionProvider> restrictions = new ArrayList<IFileRestrictionProvider>();

	/**
	 * Assigns the virtual root for the context
	 * 
	 * @param root Virtual root string
	 */
	public void VirtualRoot(String root) {
		virtualRoot = root;
	}

	/**
	 * Configures server handlers
	 * 
	 * @param handlerConfigClosure Server handler configuration closure
	 */
	public void ContextHandlers(Closure<?> handlerConfigClosure) {
		ContextHandlers(HandlerConfig.fromClosure(handlerConfigClosure));
	}

	/**
	 * Configures server handlers
	 * 
	 * @param handlerConfig Server handler configuration
	 */
	public void ContextHandlers(HandlerConfig handlerConfig) {
		handlers.addAll(handlerConfig.handlers);
	}

	/**
	 * Configures aliases
	 * 
	 * @param aliasConfigClosure Alias configuration closure
	 */
	public void Aliases(Closure<?> aliasConfigClosure) {
		Aliases(AliasConfig.fromClosure(aliasConfigClosure));
	}

	/**
	 * Configures aliases
	 * 
	 * @param aliasConfig Alias configuration
	 */
	public void Aliases(AliasConfig aliasConfig) {
		aliases.addAll(aliasConfig.aliases);
	}

	/**
	 * Configures post processors
	 * 
	 * @param postProcessorConfigClosure Post processor configuration closure
	 */
	public void PostProcessors(Closure<?> postProcessorConfigClosure) {
		PostProcessors(PostProcessorConfig.fromClosure(postProcessorConfigClosure));
	}

	/**
	 * Configures post processors
	 * 
	 * @param postProcessorConfig Post processor configuration
	 */
	public void PostProcessors(PostProcessorConfig postProcessorConfig) {
		postProcessors.addAll(postProcessorConfig.postProcessors);
	}

	/**
	 * Configures upload handlers
	 * 
	 * @param uploadHandlersConfigClosure Upload handler configuration closure
	 */
	public void UploadHandlers(Closure<?> uploadHandlersConfigClosure) {
		UploadHandlers(UploadHandlerConfig.fromClosure(uploadHandlersConfigClosure));
	}

	/**
	 * Configures upload handlers
	 * 
	 * @param uploadHandlersConfig Upload handler configuration
	 */
	public void UploadHandlers(UploadHandlerConfig uploadHandlersConfig) {
		uploadHandlers.addAll(uploadHandlersConfig.uploadHandlers);
	}

	/**
	 * Configures restrictions
	 * 
	 * @param restrictionConfigClosure Restriction configuration closure
	 */
	public void Restrictions(Closure<?> restrictionConfigClosure) {
		Restrictions(RestrictionConfig.fromClosure(restrictionConfigClosure));
	}

	/**
	 * Configures restrictions
	 * 
	 * @param restrictionConfig Restriction configuration
	 */
	public void Restrictions(RestrictionConfig restrictionConfig) {
		restrictions.addAll(restrictionConfig.restrictions);
	}

	public static ContextConfig fromClosure(Closure<?> closure) {
		ContextConfig conf = new ContextConfig();
		closure.setDelegate(conf);
		closure.call();
		return conf;
	}
}
