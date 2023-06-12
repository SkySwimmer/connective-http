package org.asf.connective.standalone.configuration;

import java.util.ArrayList;
import groovy.lang.Closure;

public class HostConfig {

	public ArrayList<HostEntry> hosts = new ArrayList<HostEntry>();

	/**
	 * Creates a new host
	 * 
	 * @param hostConfig Host configuration closure
	 */
	public void Host(Closure<?> hostConfig) {
		// Add a host
		hosts.add(HostEntry.fromClosure(hostConfig));
	}

	/**
	 * Creates a new host
	 * 
	 * @param hostConfig Host configuration
	 */
	public void Host(HostEntry hostConfig) {
		// Add a host
		hosts.add(hostConfig);
	}

	public HostConfig addFromClosure(Closure<?> closure) {
		closure.setDelegate(this);
		closure.call();
		return this;
	}

}
