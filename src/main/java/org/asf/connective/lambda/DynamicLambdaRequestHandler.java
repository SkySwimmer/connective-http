package org.asf.connective.lambda;

import java.io.IOException;

public interface DynamicLambdaRequestHandler {

	/**
	 * Called to handle the request
	 * 
	 * @param ctx Request context
	 * @throws IOException If an error occurs
	 * @return True if handled, false otherwise, return false to fall through to the
	 *         next handler
	 */
	public boolean handle(LambdaRequestContext ctx) throws IOException;

}
