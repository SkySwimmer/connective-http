package org.asf.connective;

import java.io.IOException;

import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * HTTP Content Source - Called to process HTTP requests
 * 
 * @author Sky Swimmer
 *
 */
public abstract class ContentSource {
	ContentSource parent;
	private Unsafe unsafe = new Unsafe();

	public class Unsafe {
		public void unsafeSetParent(ContentSource parent) {
			ContentSource.this.parent = parent;
		}
	}

	public Unsafe unsafe() {
		return unsafe;
	}

	/**
	 * Retrieves the parent content source
	 * 
	 * @return ContentSource instance or null
	 */
	protected ContentSource getParent() {
		return parent;
	}

	/**
	 * Runs parent content sources if present, use this for response handling
	 * fallthrough
	 * 
	 * @param path     Request path
	 * @param request  Request object
	 * @param response Response output object
	 * @param client   Client making the request
	 * @param server   Server instance
	 * @return True if successful, false otherwise
	 * @throws IOException If processing fails
	 * 
	 * @since Connective 1.0.0.A17
	 */
	protected boolean runParent(String path, HttpRequest request, HttpResponse response, RemoteClient client,
			ConnectiveHttpServer server) throws IOException {
		if (parent != null)
			return parent.process(path, request, response, client, server);
		return false;
	}

	/**
	 * Processes HTTP requests. <b>Note:</b> It is highly recommended to instead of
	 * returning false, to return runParent() if you wish to delegate to a parent
	 * content source, by returning runParent() instead of returning false, the
	 * content source will attempt to fall through to the next content source and
	 * only return false, causing 404, if none are left.
	 * 
	 * @param path     Request path
	 * @param request  Request object
	 * @param response Response output object
	 * @param client   Client making the request
	 * @param server   Server instance
	 * @return True if successful, false otherwise
	 * @throws IOException If processing fails
	 */
	public abstract boolean process(String path, HttpRequest request, HttpResponse response, RemoteClient client,
			ConnectiveHttpServer server) throws IOException;

}
