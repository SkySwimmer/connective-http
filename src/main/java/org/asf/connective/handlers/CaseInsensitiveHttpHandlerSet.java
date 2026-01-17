package org.asf.connective.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * Case-insensitive version of the {@link HttpHandlerSet HttpHandlerSet} class,
 * works the same except for that it accepts different casing during processing
 * as well
 * 
 * @author Sky Swimmer
 * @since Connective 1.0.0.A17
 * 
 */
public class CaseInsensitiveHttpHandlerSet extends HttpHandlerSet {

	private HashMap<String, ArrayList<DynamicHttpRequestHandler>> handlersSpecific = new HashMap<String, ArrayList<DynamicHttpRequestHandler>>();
	private HashMap<String, ArrayList<DynamicHttpRequestHandler>> handlersWildcard = new HashMap<String, ArrayList<DynamicHttpRequestHandler>>();

	@Override
	public DynamicHttpRequestHandler[] getAllRequestHandlers() {
		ArrayList<DynamicHttpRequestHandler> allHandlers = new ArrayList<DynamicHttpRequestHandler>();
		synchronized (handlersSpecific) {
			for (String key : handlersSpecific.keySet())
				allHandlers.addAll(handlersSpecific.get(key));
		}
		synchronized (handlersWildcard) {
			for (String key : handlersWildcard.keySet())
				allHandlers.addAll(handlersWildcard.get(key));
		}
		return allHandlers.toArray(new DynamicHttpRequestHandler[0]);
	}

	@Override
	public void registerHandler(DynamicHttpRequestHandler handler) {
		// Add specific
		String pth = sanitizePath(handler.path()).toLowerCase();
		synchronized (handlersSpecific) {
			ArrayList<DynamicHttpRequestHandler> lst = handlersSpecific.get(pth);
			if (lst == null)
				lst = new ArrayList<DynamicHttpRequestHandler>();
			lst.add(handler);
			handlersSpecific.put(pth, lst);
		}

		// If needed, add wildcard
		if (handler.supportsChildPaths()) {
			synchronized (handlersWildcard) {
				ArrayList<DynamicHttpRequestHandler> lst = handlersWildcard.get(pth);
				if (lst == null)
					lst = new ArrayList<DynamicHttpRequestHandler>();
				lst.add(handler);
				handlersWildcard.put(pth, lst);
			}
		}
	}

	@Override
	public boolean handleHttp(String path, ConnectiveHttpServer server, RemoteClient client, HttpRequest request,
			HttpResponse response) throws IOException {
		// Sanitize
		path = sanitizePath(path);

		// Load handlers
		boolean[] compatible = new boolean[] { false, false };
		DynamicHttpRequestHandler[] handlersSpecificLst = new DynamicHttpRequestHandler[0];
		synchronized (handlersSpecific) {
			if (handlersSpecific.containsKey(path.toLowerCase())) {
				handlersSpecificLst = handlersSpecific.get(path.toLowerCase())
						.toArray(new DynamicHttpRequestHandler[0]);
			}
		}

		// Find handler
		compatible = findHandler(path, server, client, request, response, handlersSpecificLst, false, false);
		boolean hadIncompatibleMethod = compatible[1];
		if (!compatible[0]) {
			compatible = findHandler(path, server, client, request, response, handlersSpecificLst, false, true);
			if (compatible[1])
				hadIncompatibleMethod = true;
		}

		// Check result
		if (!compatible[0]) {
			// Couldnt found one using strict comparison
			// Find using loose comparison
			String pth2 = path;
			while (!compatible[0]) {
				// Get list
				DynamicHttpRequestHandler[] handlersWildcardLst = new DynamicHttpRequestHandler[0];
				synchronized (handlersWildcard) {
					if (handlersWildcard.containsKey(pth2.toLowerCase())) {
						handlersWildcardLst = handlersWildcard.get(pth2.toLowerCase())
								.toArray(new DynamicHttpRequestHandler[0]);
					}
				}

				// Check list, and find handler
				if (handlersWildcardLst.length != 0)
					compatible = findHandler(pth2, server, client, request, response, handlersWildcardLst, true, false);
				if (compatible[1])
					hadIncompatibleMethod = true;

				// Check result
				if (!compatible[0]) {
					// Check root
					if (pth2.equals("/"))
						break;

					// Go up
					pth2 = pth2.substring(0, pth2.lastIndexOf("/"));
					if (pth2.isEmpty())
						pth2 = "/";
				}
			}
		}

		// Check result
		if (!compatible[0]) {
			// Couldnt found one using strict comparison
			// Find using loose comparison
			String pth2 = path;
			while (!compatible[0]) {
				// Get list
				DynamicHttpRequestHandler[] handlersWildcardLst = new DynamicHttpRequestHandler[0];
				synchronized (handlersWildcard) {
					if (handlersWildcard.containsKey(pth2.toLowerCase())) {
						handlersWildcardLst = handlersWildcard.get(pth2.toLowerCase())
								.toArray(new DynamicHttpRequestHandler[0]);
					}
				}

				// Check list, and find handler
				if (handlersWildcardLst.length != 0)
					compatible = findHandler(pth2, server, client, request, response, handlersWildcardLst, true, true);
				if (compatible[1])
					hadIncompatibleMethod = true;

				// Check result
				if (!compatible[0]) {
					// Check root
					if (pth2.equals("/"))
						break;

					// Go up
					pth2 = pth2.substring(0, pth2.lastIndexOf("/"));
					if (pth2.isEmpty())
						pth2 = "/";
				}
			}
		}

		// Check
		if (!compatible[0] && hadIncompatibleMethod && !response.wasStatusAssigned())
			response.setResponseStatus(405, "Method Not Allowed");

		// Return result, if we were able to process or not
		return compatible[0];
	}

}
