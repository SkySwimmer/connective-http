package org.asf.connective.standalone.configuration;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.asf.connective.RemoteClient;
import org.asf.connective.handlers.HttpPushHandler;
import org.asf.connective.handlers.HttpRequestHandler;
import org.asf.connective.standalone.ConnectiveStandaloneMain;

import groovy.lang.Closure;

public class HandlerConfig {

	public ArrayList<HttpRequestHandler> handlers = new ArrayList<HttpRequestHandler>();

	public static class ClosureHttpRequestHandler extends HttpRequestHandler {

		private String path;
		private boolean supportsChildPaths;
		private Closure<?> handlerClosure;

		@Override
		public HttpRequestHandler createNewInstance() {
			return new ClosureHttpRequestHandler(path, supportsChildPaths, (Closure<?>) handlerClosure.clone());
		}

		public ClosureHttpRequestHandler(String path, boolean supportsChildPaths, Closure<?> handlerClosure) {
			this.path = path;
			this.supportsChildPaths = supportsChildPaths;
			this.handlerClosure = handlerClosure;
			handlerClosure.setDelegate(this);
		}

		@Override
		public void handle(String path, String method, RemoteClient client) throws IOException {
			handlerClosure.call(path, method, client);
		}

		@Override
		public boolean supportsChildPaths() {
			return supportsChildPaths;
		}

		@Override
		public String path() {
			return path;
		}

	}

	public static class ClosureHttpPushHandler extends HttpPushHandler {

		private String path;
		private boolean supportsChildPaths;
		private boolean supportsNonPush;
		private Closure<?> handlerClosure;

		@Override
		public HttpPushHandler createNewInstance() {
			return new ClosureHttpPushHandler(path, supportsNonPush, supportsChildPaths,
					(Closure<?>) handlerClosure.clone());
		}

		public ClosureHttpPushHandler(String path, boolean supportsNonPush, boolean supportsChildPaths,
				Closure<?> handlerClosure) {
			this.path = path;
			this.supportsChildPaths = supportsChildPaths;
			this.handlerClosure = handlerClosure;
			this.supportsNonPush = supportsNonPush;
			handlerClosure.setDelegate(this);
		}

		@Override
		public void handle(String path, String method, RemoteClient client, String contentType) throws IOException {
			handlerClosure.call(path, method, client, contentType);
		}

		@Override
		public boolean supportsNonPush() {
			return supportsNonPush;
		}

		@Override
		public boolean supportsChildPaths() {
			return supportsChildPaths;
		}

		@Override
		public String path() {
			return path;
		}

	}

	/**
	 * Adds server handlers
	 * 
	 * @param handler Handler to add
	 */
	public void Handler(HttpRequestHandler handler) {
		if (!handlers.stream().anyMatch(t -> t.getClass().getTypeName().equals(handler.getClass().getTypeName())
				&& t.supportsChildPaths() == handler.supportsChildPaths() && t.path() == handler.path()))
			handlers.add(handler);
	}

	/**
	 * Adds server handlers
	 * 
	 * @param handler Handler to add
	 */
	public void Handler(HttpPushHandler handler) {
		Handler(handler);
	}

	/**
	 * Adds server handlers
	 * 
	 * @param handlerClassName Handler class name
	 * @param initParams       Constructor parameters
	 */
	public void Handler(String handlerClassName, Object... initParams) {
		try {
			Class<?> cls = Class.forName(handlerClassName, true, ConnectiveStandaloneMain.getModuleClassLoader());
			if (HttpRequestHandler.class.isAssignableFrom(cls)) {
				try {
					Handler((HttpRequestHandler) cls
							.getConstructor(Stream.of(initParams).map(t -> t.getClass()).toArray(t -> new Class[t]))
							.newInstance(initParams));
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new IllegalArgumentException("Invalid handler class name: " + handlerClassName
							+ ": no constructor matching the passed arguments");
				}
			} else
				throw new IllegalArgumentException(
						"Invalid handler class name: " + handlerClassName + ": not a handler type");
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Invalid handler class name: " + handlerClassName);
		}
	}

	/**
	 * Adds server handlers
	 * 
	 * @param path           Handler path
	 * @param handlerClosure Handler closure to add
	 */
	public void Handler(String path, Closure<?> handlerClosure) {
		Handler(new ClosureHttpRequestHandler(path, false, handlerClosure));
	}

	/**
	 * Adds server handlers
	 * 
	 * @param path               Handler path
	 * @param supportsChildPaths True to support child paths, false otherwise
	 * @param handlerClosure     Handler closure to add
	 */
	public void Handler(String path, boolean supportsChildPaths, Closure<?> handlerClosure) {
		Handler(new ClosureHttpRequestHandler(path, supportsChildPaths, handlerClosure));
	}

	/**
	 * Adds server handlers
	 * 
	 * @param path           Handler path
	 * @param handlerClosure Handler closure to add
	 * @param pushHandler    True to make this a push handler, false for a request
	 *                       handler
	 */
	public void Handler(String path, Closure<?> handlerClosure, boolean pushHandler) {
		if (pushHandler)
			Handler(new ClosureHttpPushHandler(path, false, false, handlerClosure));
		else
			Handler(new ClosureHttpRequestHandler(path, false, handlerClosure));
	}

	/**
	 * Adds server handler
	 * 
	 * @param path               Handler path
	 * @param supportsChildPaths True to support child paths, false otherwise
	 * @param handlerClosure     Handler closure to add
	 * @param pushHandler        True to make this a push handler, false for a
	 *                           request handler
	 */
	public void Handler(String path, boolean supportsChildPaths, Closure<?> handlerClosure, boolean pushHandler) {
		Handler(new ClosureHttpPushHandler(path, false, supportsChildPaths, handlerClosure));
	}

	/**
	 * Adds server handlers
	 * 
	 * @param path            Handler path
	 * @param handlerClosure  Handler closure to add
	 * @param pushHandler     True to make this a push handler, false for a request
	 *                        handler
	 * @param supportsNonPush True to support non-push requests (such as GET), false
	 *                        to reject non-push requests
	 */
	public void Handler(String path, Closure<?> handlerClosure, boolean pushHandler, boolean supportsNonPush) {
		if (pushHandler)
			Handler(new ClosureHttpPushHandler(path, supportsNonPush, false, handlerClosure));
		else
			Handler(new ClosureHttpRequestHandler(path, false, handlerClosure));
	}

	/**
	 * Adds server handler
	 * 
	 * @param path               Handler path
	 * @param supportsChildPaths True to support child paths, false otherwise
	 * @param handlerClosure     Handler closure to add
	 * @param pushHandler      True to make this a push handler, false for a
	 *                           request handler
	 * @param supportsNonPush    True to support non-push requests (such as GET),
	 *                           false to reject non-push requests
	 */
	public void Handler(String path, boolean supportsChildPaths, Closure<?> handlerClosure, boolean pushHandler,
			boolean supportsNonPush) {
		if (pushHandler)
			Handler(new ClosureHttpPushHandler(path, supportsNonPush, supportsChildPaths, handlerClosure));
		Handler(new ClosureHttpRequestHandler(path, supportsChildPaths, handlerClosure));
	}

	public static HandlerConfig fromClosure(Closure<?> closure) {
		HandlerConfig conf = new HandlerConfig();
		closure.setDelegate(conf);
		closure.call();
		return conf;
	}
}
