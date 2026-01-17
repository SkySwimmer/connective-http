package org.asf.connective.lambda;

import java.io.IOException;

public interface LambdaRequestHandler {

	/**
	 * Called to handle the request
	 * 
	 * @param ctx Request context
	 * @throws IOException If an error occurs
	 */
	public void handle(LambdaRequestContext ctx) throws IOException;

}
