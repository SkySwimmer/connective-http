package org.asf.connective.lambda;

import java.io.IOException;

public interface LambdaPushHandlerMatcher {

	/**
	 * Called to match the request handler to the resource
	 * 
	 * @param ctx Request context
	 * @throws IOException If an error occurs
	 * @return True if compatible with the handler, false to fall through to the
	 *         next handler
	 */
	public boolean match(LambdaPushContext ctx) throws IOException;

}
