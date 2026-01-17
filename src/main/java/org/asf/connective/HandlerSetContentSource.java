package org.asf.connective;

import java.io.IOException;

import org.asf.connective.handlers.HttpHandlerSet;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * Content source for a {@link HttpHandlerSet HttpHandlerSet}
 * 
 * @author Sky Swimmer
 * @since Connective 1.0.0.A17
 * 
 */
public class HandlerSetContentSource extends ContentSource {

	private HttpHandlerSet set;

	public HandlerSetContentSource(HttpHandlerSet set) {
		this.set = set;
	}

	@Override
	public boolean process(String path, HttpRequest request, HttpResponse response, RemoteClient client,
			ConnectiveHttpServer server) throws IOException {
		if (!set.handleHttp(path, server, client, request, response))
			return runParent(path, request, response, client, server);
		return true;
	}

}
