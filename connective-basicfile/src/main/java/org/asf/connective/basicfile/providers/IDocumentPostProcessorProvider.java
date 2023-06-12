package org.asf.connective.basicfile.providers;

import java.util.function.Consumer;

import org.asf.connective.RemoteClient;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * HTTP Document Post-Processor -- run to amend any response of the text/html
 * type before sending to the client. (useful for global warnings)
 * 
 * @author Sky Swimmer
 *
 */
public interface IDocumentPostProcessorProvider {

	/**
	 * Creates a new instance of the document post-processor provider
	 * 
	 * @return New IDocumentPostProcessor instance
	 */
	public IDocumentPostProcessorProvider createInstance();

	/**
	 * Checks if the given request should be post-processed
	 * 
	 * @param request   Request instance
	 * @param inputPath Request path (potentially aliased by another alias)
	 * @return True if the request alias is valid, false otherwise
	 */
	public boolean match(HttpRequest request, String inputPath);

	/**
	 * Runs the post-processor
	 * 
	 * @param path     Input path (real path in the context folder)
	 * @param request  HTTP Request
	 * @param response HTTP Response
	 * @param client   Client making the request
	 * @param method   Request method
	 */
	public void process(String path, HttpRequest request, HttpResponse response, RemoteClient client, String method);

	/**
	 * Appends to the end of the document text
	 * 
	 * @param text Text to append
	 */
	public default void writeLine(String text) {
		write(text + "\n");
	}

	/**
	 * Appends to the end of the document text
	 * 
	 * @param text Text to append
	 */
	public default void write(String text) {
		getWriteCallback().accept(text);
	}

	/**
	 * Defines if the processor should allow non-html pages, <b>note that when the
	 * page is not a HTML, you cannot append to the document</b>
	 * 
	 * @return True if this processor works with non-html responses, false otherwise
	 */
	public default boolean acceptNonHTML() {
		return false;
	}

	/**
	 * Assigns the document append callback
	 * 
	 * @param callback Callback function
	 */
	public void setWriteCallback(Consumer<String> callback);

	/**
	 * Retrieves the document append callback
	 * 
	 * @return Callback function
	 */
	public Consumer<String> getWriteCallback();
}
