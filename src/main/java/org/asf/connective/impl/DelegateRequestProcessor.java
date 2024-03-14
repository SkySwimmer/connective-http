package org.asf.connective.impl;

import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.lambda.LambdaRequestContext;
import org.asf.connective.lambda.LambdaRequestProcessor;
import org.asf.connective.processors.HttpRequestProcessor;

public class DelegateRequestProcessor extends HttpRequestProcessor {

	private String path;
	private LambdaRequestProcessor processor;
	private boolean supportsChildPaths;

	public DelegateRequestProcessor(String path, LambdaRequestProcessor processor, boolean supportsChildPaths) {
		this.path = path;
		this.processor = processor;
		this.supportsChildPaths = supportsChildPaths;
	}

	@Override
	public void process(String path, String method, RemoteClient client) throws IOException {
		processor.process(new LambdaRequestContext(client, getRequest(), getResponse(), getServer()));
	}

	@Override
	public HttpRequestProcessor createNewInstance() {
		return new DelegateRequestProcessor(path, processor, supportsChildPaths);
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
