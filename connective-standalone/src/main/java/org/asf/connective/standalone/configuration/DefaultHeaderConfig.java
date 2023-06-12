package org.asf.connective.standalone.configuration;

import org.asf.connective.headers.HeaderCollection;

import groovy.lang.Closure;

public class DefaultHeaderConfig {
	public HeaderCollection headers = new HeaderCollection();

	/**
	 * Adds HTTP headers
	 * 
	 * @param name  Header name
	 * @param value Header value
	 */
	public void Header(String name, String value) {
		headers.addHeader(name, value);
	}

	/**
	 * Adds HTTP headers
	 * 
	 * @param name   Header name
	 * @param value  Header value
	 * @param append True to add to the existing header if present, false to
	 *               overwrite values (clears the header if already present)
	 */
	public void Header(String name, String value, boolean append) {
		headers.addHeader(name, value, append);
	}

	public static DefaultHeaderConfig addFromClosure(Closure<?> closure, HeaderCollection col) {
		DefaultHeaderConfig conf = new DefaultHeaderConfig();
		conf.headers = col;
		closure.setDelegate(conf);
		closure.call();
		return conf;
	}
}
