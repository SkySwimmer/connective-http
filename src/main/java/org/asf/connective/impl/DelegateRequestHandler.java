package org.asf.connective.impl;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.handlers.HttpRequestHandler;
import org.asf.connective.lambda.LambdaRequestHandlerMatcher;
import org.asf.connective.lambda.LambdaRequestContext;
import org.asf.connective.lambda.LambdaRequestHandler;

public class DelegateRequestHandler extends HttpRequestHandler {

	private String path;
	private LambdaRequestHandlerMatcher matcher;
	private LambdaRequestHandler handler;
	private boolean supportsChildPaths;
	private String[] methods;

	@Override
	public String[] methods() {
		return methods;
	}

	public DelegateRequestHandler(String path, LambdaRequestHandlerMatcher matcher, LambdaRequestHandler handler,
			boolean supportsChildPaths, String[] methods) {
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
	public void handle(String path, String method, RemoteClient client) throws IOException {
		handler.handle(new LambdaRequestContext(client, getRequest(), getResponse(), getServer()));
	}

	@Override
	public HttpRequestHandler createNewInstance() {
		return new DelegateRequestHandler(path, matcher, handler, supportsChildPaths, methods);
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
