package org.asf.connective.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.impl.DelegatePushHandler;
import org.asf.connective.impl.DelegateRequestHandler;
import org.asf.connective.lambda.DynamicLambdaPushHandler;
import org.asf.connective.lambda.DynamicLambdaRequestHandler;
import org.asf.connective.lambda.LambdaPushHandler;
import org.asf.connective.lambda.LambdaPushHandlerMatcher;
import org.asf.connective.lambda.LambdaRequestHandler;
import org.asf.connective.lambda.LambdaRequestHandlerMatcher;
import org.asf.connective.logger.ConnectiveLogMessage;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.connective.impl.DynamicDelegatePushHandler;
import org.asf.connective.impl.DynamicDelegateRequestHandler;

/**
 * 
 * HttpHandlerSet class, a class to manage and use connective Handler objects
 * for the content source, vhost, shard or server (depending on the setup), all
 * HTTP handlers ultimately get registered in a handler set, which are then used
 * by a content source to implement API calls, this is the class where you will
 * want to register calls to.
 * 
 * @author Sky Swimmer
 * @since Connective 1.0.0.A17
 * 
 */
public class HttpHandlerSet {

	private HashMap<String, ArrayList<DynamicHttpRequestHandler>> handlersSpecific = new HashMap<String, ArrayList<DynamicHttpRequestHandler>>();
	private HashMap<String, ArrayList<DynamicHttpRequestHandler>> handlersWildcard = new HashMap<String, ArrayList<DynamicHttpRequestHandler>>();

	/**
	 * Retrieves all registered HTTP request handlers
	 * 
	 * @return Array of DynamicHttpRequestHandler instances
	 */
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

	/**
	 * Registers a new request handler
	 * 
	 * @param path    The path to register to
	 * @param handler Handler call
	 * @param methods HTTP methods that are supported
	 */
	public void registerHandler(String path, LambdaRequestHandler handler, String... methods) {
		registerHandler(path, handler, false, methods);
	}

	/**
	 * Registers a new request handler
	 * 
	 * @param path    The path to register to
	 * @param handler Handler call
	 * @param methods HTTP methods that are supported
	 */
	public void registerHandler(String path, DynamicLambdaRequestHandler handler, String... methods) {
		registerHandler(path, handler, false, methods);
	}

	/**
	 * Registers a new request handler
	 * 
	 * @param path               The path to register to
	 * @param handler            Handler call
	 * @param supportsChildPaths True to supports child paths, false otherwise
	 * @param methods            HTTP methods that are supported
	 */
	public void registerHandler(String path, LambdaRequestHandler handler, boolean supportsChildPaths,
			String... methods) {
		registerHandler(new DelegateRequestHandler(path, null, handler, supportsChildPaths, methods));
	}

	/**
	 * Registers a new request handler
	 * 
	 * @param path               The path to register to
	 * @param handler            Handler call
	 * @param supportsChildPaths True to supports child paths, false otherwise
	 */
	public void registerHandler(String path, DynamicLambdaRequestHandler handler, boolean supportsChildPaths,
			String... methods) {
		registerHandler(new DynamicDelegateRequestHandler(path, null, handler, supportsChildPaths, methods));
	}

	/**
	 * Registers a new request handler
	 * 
	 * @param path    The path to register to
	 * @param matcher Matcher call
	 * @param handler Handler call
	 * @param methods HTTP methods that are supported
	 */
	public void registerHandler(String path, LambdaRequestHandlerMatcher matcher, LambdaRequestHandler handler,
			String... methods) {
		registerHandler(path, matcher, handler, false, methods);
	}

	/**
	 * Registers a new request handler
	 * 
	 * @param path    The path to register to
	 * @param matcher Matcher call
	 * @param handler Handler call
	 * @param methods HTTP methods that are supported
	 */
	public void registerHandler(String path, LambdaRequestHandlerMatcher matcher, DynamicLambdaRequestHandler handler,
			String... methods) {
		registerHandler(path, matcher, handler, false, methods);
	}

	/**
	 * Registers a new request handler
	 * 
	 * @param path               The path to register to
	 * @param matcher            Matcher call
	 * @param handler            Handler call
	 * @param supportsChildPaths True to supports child paths, false otherwise
	 * @param methods            HTTP methods that are supported
	 */
	public void registerHandler(String path, LambdaRequestHandlerMatcher matcher, LambdaRequestHandler handler,
			boolean supportsChildPaths, String... methods) {
		registerHandler(new DelegateRequestHandler(path, matcher, handler, supportsChildPaths, methods));
	}

	/**
	 * Registers a new request handler
	 * 
	 * @param path               The path to register to
	 * @param matcher            Matcher call
	 * @param handler            Handler call
	 * @param supportsChildPaths True to supports child paths, false otherwise
	 * @param methods            HTTP methods that are supported
	 */
	public void registerHandler(String path, LambdaRequestHandlerMatcher matcher, DynamicLambdaRequestHandler handler,
			boolean supportsChildPaths, String... methods) {
		registerHandler(new DynamicDelegateRequestHandler(path, matcher, handler, supportsChildPaths, methods));
	}

	/**
	 * Registers a new push handler
	 * 
	 * @param path    The path to register to
	 * @param handler Handler call
	 * @param methods HTTP methods that are supported
	 */
	public void registerHandler(String path, LambdaPushHandler handler, String... methods) {
		registerHandler(path, null, handler, false, methods);
	}

	/**
	 * Registers a new push handler
	 * 
	 * @param path    The path to register to
	 * @param handler Handler call
	 * @param methods HTTP methods that are supported
	 */
	public void registerHandler(String path, DynamicLambdaPushHandler handler, String... methods) {
		registerHandler(path, null, handler, false, methods);
	}

	/**
	 * Registers a new push handler
	 * 
	 * @param path               The path to register to
	 * @param handler            Handler call
	 * @param supportsChildPaths True to supports child paths, false otherwise
	 * @param methods            HTTP methods that are supported
	 */
	public void registerHandler(String path, LambdaPushHandler handler, boolean supportsChildPaths, String... methods) {
		registerHandler(path, null, handler, supportsChildPaths, false, methods);
	}

	/**
	 * Registers a new push handler
	 * 
	 * @param path               The path to register to
	 * @param handler            Handler call
	 * @param supportsChildPaths True to supports child paths, false otherwise
	 * @param methods            HTTP methods that are supported
	 */
	public void registerHandler(String path, DynamicLambdaPushHandler handler, boolean supportsChildPaths,
			String... methods) {
		registerHandler(path, null, handler, supportsChildPaths, false, methods);
	}

	/**
	 * Registers a new push handler
	 * 
	 * @param path               The path to register to
	 * @param handler            Handler call
	 * @param supportsChildPaths True to supports child paths, false otherwise
	 * @param supportsNonPush    True to support non-upload requests, false
	 *                           otherwise
	 * @param methods            HTTP methods that are supported
	 */
	public void registerHandler(String path, LambdaPushHandler handler, boolean supportsChildPaths,
			boolean supportsNonPush, String... methods) {
		registerHandler(new DelegatePushHandler(path, null, handler, supportsChildPaths, supportsNonPush, methods));
	}

	/**
	 * Registers a new push handler
	 * 
	 * @param path               The path to register to
	 * @param handler            Handler call
	 * @param supportsChildPaths True to supports child paths, false otherwise
	 * @param supportsNonPush    True to support non-upload requests, false
	 *                           otherwise
	 * @param methods            HTTP methods that are supported
	 */
	public void registerHandler(String path, DynamicLambdaPushHandler handler, boolean supportsChildPaths,
			boolean supportsNonPush, String... methods) {
		registerHandler(
				new DynamicDelegatePushHandler(path, null, handler, supportsChildPaths, supportsNonPush, methods));
	}

	/**
	 * Registers a new push handler
	 * 
	 * @param path    The path to register to
	 * @param matcher Matcher call
	 * @param handler Handler call
	 * @param methods HTTP methods that are supported
	 */
	public void registerHandler(String path, LambdaPushHandlerMatcher matcher, LambdaPushHandler handler,
			String... methods) {
		registerHandler(path, matcher, handler, false, methods);
	}

	/**
	 * Registers a new push handler
	 * 
	 * @param path    The path to register to
	 * @param matcher Matcher call
	 * @param handler Handler call
	 * @param methods HTTP methods that are supported
	 */
	public void registerHandler(String path, LambdaPushHandlerMatcher matcher, DynamicLambdaPushHandler handler,
			String... methods) {
		registerHandler(path, matcher, handler, false, methods);
	}

	/**
	 * Registers a new push handler
	 * 
	 * @param path               The path to register to
	 * @param matcher            Matcher call
	 * @param handler            Handler call
	 * @param supportsChildPaths True to supports child paths, false otherwise
	 * @param methods            HTTP methods that are supported
	 */
	public void registerHandler(String path, LambdaPushHandlerMatcher matcher, LambdaPushHandler handler,
			boolean supportsChildPaths, String... methods) {
		registerHandler(path, matcher, handler, supportsChildPaths, false, methods);
	}

	/**
	 * Registers a new push handler
	 * 
	 * @param path               The path to register to
	 * @param matcher            Matcher call
	 * @param handler            Handler call
	 * @param supportsChildPaths True to supports child paths, false otherwise
	 * @param methods            HTTP methods that are supported
	 */
	public void registerHandler(String path, LambdaPushHandlerMatcher matcher, DynamicLambdaPushHandler handler,
			boolean supportsChildPaths, String... methods) {
		registerHandler(path, matcher, handler, supportsChildPaths, false, methods);
	}

	/**
	 * Registers a new push handler
	 * 
	 * @param path               The path to register to
	 * @param matcher            Matcher call
	 * @param handler            Handler call
	 * @param supportsChildPaths True to supports child paths, false otherwise
	 * @param supportsNonPush    True to support non-upload requests, false
	 *                           otherwise
	 * @param methods            HTTP methods that are supported
	 */
	public void registerHandler(String path, LambdaPushHandlerMatcher matcher, LambdaPushHandler handler,
			boolean supportsChildPaths, boolean supportsNonPush, String... methods) {
		registerHandler(new DelegatePushHandler(path, matcher, handler, supportsChildPaths, supportsNonPush, methods));
	}

	/**
	 * Registers a new push handler
	 * 
	 * @param path               The path to register to
	 * @param matcher            Matcher call
	 * @param handler            Handler call
	 * @param supportsChildPaths True to supports child paths, false otherwise
	 * @param supportsNonPush    True to support non-upload requests, false
	 *                           otherwise
	 * @param methods            HTTP methods that are supported
	 */
	public void registerHandler(String path, LambdaPushHandlerMatcher matcher, DynamicLambdaPushHandler handler,
			boolean supportsChildPaths, boolean supportsNonPush, String... methods) {
		registerHandler(
				new DynamicDelegatePushHandler(path, matcher, handler, supportsChildPaths, supportsNonPush, methods));
	}

	/**
	 * Registers a new push handler
	 * 
	 * @param handler The handler implementation to register
	 */
	public void registerHandler(HttpPushHandler handler) {
		registerHandler((DynamicHttpPushHandler) handler);
	}

	/**
	 * Registers a new push handler
	 * 
	 * @param handler The handler implementation to register
	 */
	public void registerHandler(DynamicHttpPushHandler handler) {
		// Register
		registerHandler((DynamicHttpRequestHandler) handler);
	}

	/**
	 * Registers a new request handler
	 * 
	 * @param handler The handler implementation to register.
	 */
	public void registerHandler(HttpRequestHandler handler) {
		registerHandler((DynamicHttpRequestHandler) handler);
	}

	/**
	 * Registers a new request handler
	 * 
	 * @param handler The handler implementation to register.
	 */
	public void registerHandler(DynamicHttpRequestHandler handler) {
		// Add specific
		String pth = sanitizePath(handler.path());
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

	/**
	 * Processes HTTP requests using the handlers registered to this set of handlers
	 * 
	 * @param path     Request path
	 * @param server   Server handling the request (passed to the handlers)
	 * @param client   Client making the request
	 * @param request  Request object
	 * @param response Response object
	 * @return True if any handler was able to handle this request, false if none
	 *         were suitable
	 * @throws IOException If an IO error occurs in the handling of the request
	 */
	public boolean handleHttp(String path, ConnectiveHttpServer server, RemoteClient client, HttpRequest request,
			HttpResponse response) throws IOException {
		// Sanitize
		path = sanitizePath(path);

		// Load handlers
		boolean[] compatible = new boolean[] { false, false };
		DynamicHttpRequestHandler[] handlersSpecificLst = new DynamicHttpRequestHandler[0];
		synchronized (handlersSpecific) {
			if (handlersSpecific.containsKey(path)) {
				handlersSpecificLst = handlersSpecific.get(path).toArray(new DynamicHttpRequestHandler[0]);
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
					if (handlersWildcard.containsKey(pth2)) {
						handlersWildcardLst = handlersWildcard.get(pth2).toArray(new DynamicHttpRequestHandler[0]);
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
					if (handlersWildcard.containsKey(pth2)) {
						handlersWildcardLst = handlersWildcard.get(pth2).toArray(new DynamicHttpRequestHandler[0]);
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

	protected static boolean[] findHandler(String path, ConnectiveHttpServer server, RemoteClient client,
			HttpRequest request, HttpResponse response, DynamicHttpRequestHandler[] handlersForResource,
			boolean allowWithChildPaths, boolean allowWildcardmethods) throws IOException {
		boolean compatible = false;
		boolean hadIncompatibleMethod = false;

		// Check if the request has a body
		if (request.hasRequestBody()) {
			// It does, search for a Push handler for handling
			DynamicHttpRequestHandler previous = null;
			for (DynamicHttpRequestHandler p : handlersForResource) {
				// Check if the stream was touched
				if (request.wasBodyStreamTouched()) {
					// Warn
					client.getLogger()
							.warn(new ConnectiveLogMessage("handler", "Unable to fall through to next handlers for "
									+ path + ", the request handler " + previous.getClass().getTypeName()
									+ " had interacted with the request body stream, making fallthrough impossible.",
									null, client));
					break;
				}
				previous = p;

				// Check method
				if ((p.methods().length != 0 || !allowWildcardmethods)
						&& !Stream.of(p.methods()).anyMatch(t -> (allowWildcardmethods && t.equals("*"))
								|| t.equalsIgnoreCase(request.getRequestMethod()))) {
					hadIncompatibleMethod = true;
					continue;
				}

				// Filter any non-push handlers
				if (!(p instanceof DynamicHttpPushHandler) || (!allowWithChildPaths && p.supportsChildPaths()))
					continue;

				// Attempt running it
				DynamicHttpPushHandler proc = (DynamicHttpPushHandler) p;
				if (!proc.match(path, request.getRequestMethod(), client, request.getHeaderValue("Content-Type")))
					continue;
				DynamicHttpPushHandler handler = proc.instantiate(server, request, response);
				if (handler.handleRequest(path, request.getRequestMethod(), client,
						request.getHeaderValue("Content-Type"))) {
					// Success, break
					compatible = true;
					break;
				}
			}
		} else {
			// This is a request without a body, search for handlers that handle it
			DynamicHttpRequestHandler previous = null;
			for (DynamicHttpRequestHandler proc : handlersForResource) {
				// Check if the stream was touched
				if (request.wasBodyStreamTouched()) {
					// Warn
					client.getLogger()
							.warn(new ConnectiveLogMessage("handler", "Unable to fall through to next handlers for "
									+ path + ", the request handler " + previous.getClass().getTypeName()
									+ " had interacted with the request body stream, making fallthrough impossible.",
									null, client));
					break;
				}
				previous = proc;

				// Check method
				if ((proc.methods().length != 0 || !allowWildcardmethods)
						&& !Stream.of(proc.methods()).anyMatch(t -> (allowWildcardmethods && t.equals("*"))
								|| t.equalsIgnoreCase(request.getRequestMethod()))) {
					hadIncompatibleMethod = true;
					continue;
				}

				// Filter any handlers not meant to process body-less requests
				if ((proc instanceof DynamicHttpPushHandler && !((DynamicHttpPushHandler) proc).supportsNonPush())
						|| (!allowWithChildPaths && proc.supportsChildPaths())
						|| !proc.match(path, request.getRequestMethod(), client))
					continue;

				// Attempt running it
				DynamicHttpRequestHandler handler = proc.instantiate(server, request, response);
				if (handler.handleRequest(path, request.getRequestMethod(), client)) {
					// Success, break
					compatible = true;
					break;
				}
			}
		}
		return new boolean[] { compatible, hadIncompatibleMethod };
	}

	protected String sanitizePath(String path) {
		if (path.contains("\\"))
			path = path.replace("\\", "/");
		while (path.startsWith("/"))
			path = path.substring(1);
		while (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		while (path.contains("//"))
			path = path.replace("//", "/");
		path = "/" + path;
		return path;
	}

}
