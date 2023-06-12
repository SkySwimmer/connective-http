package org.asf.connective.basicfile.util;

import java.io.InputStream;

import org.asf.connective.objects.HttpResponse;

/**
 * 
 * File context - contains response information for the request.
 * 
 * @author Sky Swimmer
 *
 */
public class FileContext {
	private HttpResponse response;
	private InputStream file;
	private String mediaType;
	private long length = -1;

	protected FileContext() {
	}

	/**
	 * Creates a new file context instance
	 * 
	 * @param input     Input response
	 * @param mediaType New media type
	 * @param body      Content stream
	 * @param length    Content length
	 * @return New FileContext instance
	 */
	public static FileContext create(HttpResponse input, String mediaType, InputStream body, long length) {
		FileContext context = new FileContext();

		context.file = body;
		context.length = length;
		context.response = input;
		context.mediaType = mediaType;

		return context;
	}

	/**
	 * Creates a new file context instance
	 * 
	 * @param input     Input response
	 * @param mediaType New media type
	 * @param body      Content stream
	 * @return New FileContext instance
	 */
	public static FileContext create(HttpResponse input, String mediaType, InputStream body) {
		FileContext context = new FileContext();

		context.file = body;
		context.length = -1;
		context.response = input;
		context.mediaType = mediaType;

		return context;
	}

	/**
	 * Retrieves the current document stream
	 * 
	 * @return Document InputStream
	 */
	public InputStream getCurrentStream() {
		return file;
	}

	/**
	 * Creates the rewritten HTTP response
	 * 
	 * @return Rewritten HttpResponse instance
	 */
	public HttpResponse getRewrittenResponse() {
		response.setContent(mediaType, file, length);
		return response;
	}
}
