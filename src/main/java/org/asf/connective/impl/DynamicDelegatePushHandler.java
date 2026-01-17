package org.asf.connective.impl;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.handlers.DynamicHttpPushHandler;
import org.asf.connective.lambda.DynamicLambdaPushHandler;
import org.asf.connective.lambda.LambdaPushContext;
import org.asf.connective.lambda.LambdaPushHandlerMatcher;

public class DynamicDelegatePushHandler extends DynamicHttpPushHandler {

	private String path;
	private DynamicLambdaPushHandler handler;
	private LambdaPushHandlerMatcher matcher;
	private boolean supportsChildPaths;
	private boolean supportsNonPush;
	private String[] methods;

	@Override
	public String[] methods() {
		return methods;
	}

	public DynamicDelegatePushHandler(String path, LambdaPushHandlerMatcher matcher, DynamicLambdaPushHandler handler,
			boolean supportsChildPaths, boolean supportsNonPush, String[] methods) {
		this.path = path;
		this.handler = handler;
		this.matcher = matcher;
		this.supportsChildPaths = supportsChildPaths;
		this.supportsNonPush = supportsNonPush;
		this.methods = methods;
		if (methods.length == 0) {
			if (this.supportsNonPush)
				this.methods = new String[] { "GET", "PUT", "POST" };
			else
				this.methods = new String[] { "PUT", "POST" };
		}
	}

	@Override
	public boolean match(String path, String method, RemoteClient client, String contentType) throws IOException {
		if (matcher == null)
			return true;
		return matcher.match(new LambdaPushContext(client, getRequest(), getResponse(), getServer(), contentType));
	}

	@Override
	public boolean handleRequest(String path, String method, RemoteClient client, String contentType)
			throws IOException {
		return handler.handle(new LambdaPushContext(client, getRequest(), getResponse(), getServer(), contentType));
	}

	@Override
	public DynamicHttpPushHandler createNewInstance() {
		return new DynamicDelegatePushHandler(path, matcher, handler, supportsChildPaths, supportsNonPush, methods);
	}

	@Override
	public String path() {
		return path;
	}

	@Override
	public boolean supportsNonPush() {
		return supportsNonPush;
	}

	@Override
	public boolean supportsChildPaths() {
		return supportsChildPaths;
	}

}
