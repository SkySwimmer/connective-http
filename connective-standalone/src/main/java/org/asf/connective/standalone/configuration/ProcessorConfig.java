package org.asf.connective.standalone.configuration;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.connective.processors.HttpRequestProcessor;
import org.asf.connective.standalone.ConnectiveStandaloneMain;

import groovy.lang.Closure;

public class ProcessorConfig {

	public ArrayList<HttpRequestProcessor> processors = new ArrayList<HttpRequestProcessor>();

	public static class ClosureHttpRequestProcessor extends HttpRequestProcessor {

		private String path;
		private boolean supportsChildPaths;
		private Closure<?> processorClosure;

		@Override
		public HttpRequestProcessor createNewInstance() {
			return new ClosureHttpRequestProcessor(path, supportsChildPaths, (Closure<?>) processorClosure.clone());
		}

		public ClosureHttpRequestProcessor(String path, boolean supportsChildPaths, Closure<?> processorClosure) {
			this.path = path;
			this.supportsChildPaths = supportsChildPaths;
			this.processorClosure = processorClosure;
			processorClosure.setDelegate(this);
		}

		@Override
		public void process(String path, String method, RemoteClient client) throws IOException {
			processorClosure.call(path, method, client);
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

	public static class ClosureHttpPushProcessor extends HttpPushProcessor {

		private String path;
		private boolean supportsChildPaths;
		private boolean supportsNonPush;
		private Closure<?> processorClosure;

		@Override
		public HttpPushProcessor createNewInstance() {
			return new ClosureHttpPushProcessor(path, supportsNonPush, supportsChildPaths,
					(Closure<?>) processorClosure.clone());
		}

		public ClosureHttpPushProcessor(String path, boolean supportsNonPush, boolean supportsChildPaths,
				Closure<?> processorClosure) {
			this.path = path;
			this.supportsChildPaths = supportsChildPaths;
			this.processorClosure = processorClosure;
			this.supportsNonPush = supportsNonPush;
			processorClosure.setDelegate(this);
		}

		@Override
		public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
			processorClosure.call(path, method, client, contentType);
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
	 * Adds server processors
	 * 
	 * @param processor Processor to add
	 */
	public void Processor(HttpRequestProcessor processor) {
		if (!processors.stream().anyMatch(t -> t.getClass().getTypeName().equals(processor.getClass().getTypeName())
				&& t.supportsChildPaths() == processor.supportsChildPaths() && t.path() == processor.path()))
			processors.add(processor);
	}

	/**
	 * Adds server processors
	 * 
	 * @param processor Processor to add
	 */
	public void Processor(HttpPushProcessor processor) {
		Processor(processor);
	}

	/**
	 * Adds server processors
	 * 
	 * @param processorClassName Processor class name
	 * @param initParams         Constructor parameters
	 */
	public void Processor(String processorClassName, Object... initParams) {
		try {
			Class<?> cls = Class.forName(processorClassName, true, ConnectiveStandaloneMain.getModuleClassLoader());
			if (HttpRequestProcessor.class.isAssignableFrom(cls)) {
				try {
					Processor((HttpRequestProcessor) cls
							.getConstructor(Stream.of(initParams).map(t -> t.getClass()).toArray(t -> new Class[t]))
							.newInstance(initParams));
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new IllegalArgumentException("Invalid processor class name: " + processorClassName
							+ ": no constructor matching the passed arguments");
				}
			} else
				throw new IllegalArgumentException(
						"Invalid processor class name: " + processorClassName + ": not a processor type");
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Invalid processor class name: " + processorClassName);
		}
	}

	/**
	 * Adds server processors
	 * 
	 * @param path             Processor path
	 * @param processorClosure Processor closure to add
	 */
	public void Processor(String path, Closure<?> processorClosure) {
		Processor(new ClosureHttpRequestProcessor(path, false, processorClosure));
	}

	/**
	 * Adds server processors
	 * 
	 * @param path               Processor path
	 * @param supportsChildPaths True to support child paths, false otherwise
	 * @param processorClosure   Processor closure to add
	 */
	public void Processor(String path, boolean supportsChildPaths, Closure<?> processorClosure) {
		Processor(new ClosureHttpRequestProcessor(path, supportsChildPaths, processorClosure));
	}

	/**
	 * Adds server processors
	 * 
	 * @param path             Processor path
	 * @param processorClosure Processor closure to add
	 * @param pushProcessor    True to make this a push processor, false for a
	 *                         request processor
	 */
	public void Processor(String path, Closure<?> processorClosure, boolean pushProcessor) {
		if (pushProcessor)
			Processor(new ClosureHttpPushProcessor(path, false, false, processorClosure));
		else
			Processor(new ClosureHttpRequestProcessor(path, false, processorClosure));
	}

	/**
	 * Adds server processors
	 * 
	 * @param path               Processor path
	 * @param supportsChildPaths True to support child paths, false otherwise
	 * @param processorClosure   Processor closure to add
	 * @param pushProcessor      True to make this a push processor, false for a
	 *                           request processor
	 */
	public void Processor(String path, boolean supportsChildPaths, Closure<?> processorClosure, boolean pushProcessor) {
		Processor(new ClosureHttpPushProcessor(path, false, supportsChildPaths, processorClosure));
	}

	/**
	 * Adds server processors
	 * 
	 * @param path             Processor path
	 * @param processorClosure Processor closure to add
	 * @param pushProcessor    True to make this a push processor, false for a
	 *                         request processor
	 * @param supportsNonPush  True to support non-push requests (such as GET),
	 *                         false to reject non-push requests
	 */
	public void Processor(String path, Closure<?> processorClosure, boolean pushProcessor, boolean supportsNonPush) {
		if (pushProcessor)
			Processor(new ClosureHttpPushProcessor(path, supportsNonPush, false, processorClosure));
		else
			Processor(new ClosureHttpRequestProcessor(path, false, processorClosure));
	}

	/**
	 * Adds server processors
	 * 
	 * @param path               Processor path
	 * @param supportsChildPaths True to support child paths, false otherwise
	 * @param processorClosure   Processor closure to add
	 * @param pushProcessor      True to make this a push processor, false for a
	 *                           request processor
	 * @param supportsNonPush    True to support non-push requests (such as GET),
	 *                           false to reject non-push requests
	 */
	public void Processor(String path, boolean supportsChildPaths, Closure<?> processorClosure, boolean pushProcessor,
			boolean supportsNonPush) {
		if (pushProcessor)
			Processor(new ClosureHttpPushProcessor(path, supportsNonPush, supportsChildPaths, processorClosure));
		Processor(new ClosureHttpRequestProcessor(path, supportsChildPaths, processorClosure));
	}

	public static ProcessorConfig fromClosure(Closure<?> closure) {
		ProcessorConfig conf = new ProcessorConfig();
		closure.setDelegate(conf);
		closure.call();
		return conf;
	}
}
