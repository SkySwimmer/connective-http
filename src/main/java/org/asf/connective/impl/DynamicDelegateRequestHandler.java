package org.asf.connective.impl;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.handlers.DynamicHttpRequestHandler;
import org.asf.connective.lambda.LambdaRequestContext;
import org.asf.connective.lambda.LambdaRequestHandlerMatcher;
import org.asf.connective.lambda.DynamicLambdaRequestHandler;

public class DynamicDelegateRequestHandler extends DynamicHttpRequestHandler {

	private String path;
	private DynamicLambdaRequestHandler handler;
	private LambdaRequestHandlerMatcher matcher;
	private boolean supportsChildPaths;
	private String[] methods;

	@Override
	public String[] methods() {
		return methods;
	}

	public DynamicDelegateRequestHandler(String path, LambdaRequestHandlerMatcher matcher,
			DynamicLambdaRequestHandler handler, boolean supportsChildPaths, String[] methods) {
		this.path = path;
		this.handler = handler;
		this.matcher = matcher;
		this.supportsChildPaths = supportsChildPaths;
		this.methods = methods;
		if (methods.length == 0) {
			this.methods = new String[] { "GET" };
		}
	}

	@Override
	public boolean match(String path, String method, RemoteClient client) throws IOException {
		if (matcher == null)
			return true;
		return matcher.match(new LambdaRequestContext(client, getRequest(), getResponse(), getServer()));
	}

	@Override
	public boolean handleRequest(String path, String method, RemoteClient client) throws IOException {
		return handler.handle(new LambdaRequestContext(client, getRequest(), getResponse(), getServer()));
	}

	@Override
	public DynamicHttpRequestHandler createNewInstance() {
		return new DynamicDelegateRequestHandler(path, matcher, handler, supportsChildPaths, methods);
	}

	@Override
	public String path() {
		return path;
	}

	@Override
	public boolean supportsChildPaths() {
		return supportsChildPaths;
	}

}
