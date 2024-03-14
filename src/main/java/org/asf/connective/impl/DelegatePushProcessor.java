package org.asf.connective.impl;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.lambda.LambdaPushContext;
import org.asf.connective.lambda.LambdaPushProcessor;
import org.asf.connective.processors.HttpPushProcessor;

public class DelegatePushProcessor extends HttpPushProcessor {

	private String path;
	private LambdaPushProcessor processor;
	private boolean supportsChildPaths;
	private boolean supportsNonPush;

	public DelegatePushProcessor(String path, LambdaPushProcessor processor, boolean supportsChildPaths,
			boolean supportsNonPush) {
		this.path = path;
		this.processor = processor;
		this.supportsChildPaths = supportsChildPaths;
		this.supportsNonPush = supportsNonPush;
	}

	@Override
	public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
		processor.process(new LambdaPushContext(client, getRequest(), getResponse(), getServer(), contentType));
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new DelegatePushProcessor(path, processor, supportsChildPaths, supportsNonPush);
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
