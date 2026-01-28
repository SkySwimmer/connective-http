package org.asf.connective;

import java.io.IOException;

import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

class DefaultContentSource extends ContentSource {

	private ConnectiveHttpServer server;

	public DefaultContentSource(ConnectiveHttpServer server) {
		this.server = server;
	}

	@Override
	public boolean process(String path, HttpRequest request, HttpResponse response, RemoteClient client,
			ConnectiveHttpServer server) throws IOException {
		if (!this.server.getHandlerSet().handleHttp(path, server, client, request, response))
			return runParent(path, request, response, client, server);
		return true;
	}

}
