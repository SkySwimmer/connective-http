package org.asf.connective.lambda;

import java.io.IOException;

public interface LambdaPushHandler {

	/**
	 * Called to handle the request
	 * 
	 * @param ctx Request context
	 * @throws IOException If an error occurs
	 */
	public void handle(LambdaPushContext ctx) throws IOException;

}
