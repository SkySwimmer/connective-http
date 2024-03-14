package org.asf.connective.lambda;

import java.io.IOException;

public interface LambdaRequestProcessor {

	/**
	 * Called to process the request
	 * 
	 * @param ctx Request context
	 * @throws IOException If an error occurs
	 */
	public void process(LambdaRequestContext ctx) throws IOException;

}
