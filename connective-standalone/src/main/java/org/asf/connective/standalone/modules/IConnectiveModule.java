package org.asf.connective.standalone.modules;

import java.util.Map;
import java.util.function.Consumer;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.basicfile.FileProviderContextFactory;

import groovy.lang.Binding;

/**
 * 
 * Connective server module interface
 * 
 * @author Sky Swimmer
 *
 */
public interface IConnectiveModule {

	/**
	 * Defines the module ID
	 * 
	 * @return Module ID string
	 */
	public String moduleID();

	/**
	 * Defines the module version
	 * 
	 * @return Module version string
	 */
	public String version();

	/**
	 * Called to pre-initialize the module
	 */
	public default void preInit() {
	}

	/**
	 * Called to initialize the module
	 */
	public void init();

	/**
	 * Called to post-initialize the module, called after all modules are loaded
	 */
	public default void postInit() {
	}

	/**
	 * Called when the module configuration is loaded
	 * 
	 * @param config Module configuration map
	 */
	public default void onLoadModuleConfig(Map<String, String> config) {
	}

	/**
	 * Called when the standalone server software prepares an HTTP server
	 * 
	 * @param server Server being prepared
	 */
	public default void onPrepareServer(ConnectiveHttpServer server) {
	}

	/**
	 * Called when the standalone server software prepares server context
	 * 
	 * @param contextFactory Server context factory being prepared
	 */
	public default void onPrepareContext(FileProviderContextFactory contextFactory) {
	}

	/**
	 * Called when connective prepares a configuration script binding for running
	 * groovy configuration script code
	 * 
	 * @param binding               Binding used for the configuration
	 * @param propertyMergeCallback Callback you can call to merge a object into the
	 *                              groovy script, supports any methods and static,
	 *                              final fields
	 */
	public default void onPrepareConfigBinding(Binding binding, Consumer<Object> propertyMergeCallback) {
	}

}
