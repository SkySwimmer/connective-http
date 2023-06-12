package org.asf.connective.testserver;

import java.io.IOException;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.NetworkedConnectiveHttpServer;
import org.asf.connective.testserver.testhandlers.TestRequestProcessor;

public class TestMain {

	public static void main(String[] args) throws IOException {
		NetworkedConnectiveHttpServer server = ConnectiveHttpServer.createNetworked("HTTP/1.1");
		server.registerProcessor(new TestRequestProcessor());
		server.start();
		server.waitForExit();
	}

}
