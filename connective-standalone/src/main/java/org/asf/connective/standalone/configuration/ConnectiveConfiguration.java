package org.asf.connective.standalone.configuration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.asf.connective.ContentSource;
import org.asf.connective.basicfile.util.BasicfileContentSource;
import org.asf.connective.standalone.ConnectiveStandaloneMain;

import groovy.lang.Closure;

public class ConnectiveConfiguration {

	public HostConfig hosts = new HostConfig();
	public HashMap<String, Map<String, String>> modules = new HashMap<String, Map<String, String>>();

	public static final DefaultContentSourcesContainer DefaultContentSources = new DefaultContentSourcesContainer();

	public static class DefaultContentSourcesContainer {

		/**
		 * Basicfile-based content source
		 */
		public final ContentSource Basicfile = new BasicfileContentSource();

	}

	/**
	 * Retrieves module configurations
	 * 
	 * @param moduleID Module ID
	 * @return Module configuration map or null
	 */
	@ConfigExclude
	public Map<String, String> getModuleConfig(String moduleID) {
		return modules.get(moduleID);
	}

	/**
	 * Loads other configuration files
	 * 
	 * @param path Configuration file to load
	 */
	public void LoadConfig(String path) {
		LoadConfigFile(new File(path));
	}

	/**
	 * Loads other configuration files
	 * 
	 * @param file Configuration file to load
	 */
	public void LoadConfigFile(File file) {
		ConnectiveStandaloneMain.loadConfiguration(file);
	}

	/**
	 * Configures hosts
	 * 
	 * @param hostSettings Closure containing host settings
	 */
	public void Hosts(Closure<?> hostSettings) {
		hosts.addFromClosure(hostSettings);
	}

	/**
	 * Configures modules
	 * 
	 * @param moduleID     Module ID
	 * @param moduleConfig Module configuration closure
	 */
	public void Module(String moduleID, Closure<?> moduleConfig) {
		HashMap<String, String> properties = new HashMap<String, String>();
		moduleConfig.setDelegate(new PropertyBridge(properties));
		moduleConfig.call();
		ModuleConfig(moduleID, properties);
	}

	/**
	 * Configures modules
	 * 
	 * @param moduleID   Module ID
	 * @param properties Module configuration map
	 */
	public void ModuleConfig(String moduleID, Map<String, String> properties) {
		modules.put(moduleID, properties);
	}

}
