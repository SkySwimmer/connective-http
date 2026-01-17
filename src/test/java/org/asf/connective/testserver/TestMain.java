package org.asf.connective.testserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.function.BiFunction;
import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.NetworkedConnectiveHttpServer;
import org.asf.connective.io.IoUtil;
import org.asf.connective.lambda.LambdaPushContext;
import org.asf.connective.lambda.LambdaRequestContext;
import org.asf.connective.tasks.AsyncTaskManager;

public class TestMain {

	/*
	 * 
	 * 
	 * public static void main(String[] args) throws IOException {
	 * NetworkedConnectiveHttpServer server =
	 * ConnectiveHttpServer.createNetworked("HTTP/1.1");
	 * server.registerProcessor(new TestRequestProcessor()); server.start();
	 * server.waitForExit(); }
	 *
	 *
	 */

	public static void main(String[] args) throws IOException {
		NetworkedConnectiveHttpServer server = ConnectiveHttpServer.createNetworked("HTTP/1.1");
		server.setListenPort(8080);
		server.setListenAddress(InetAddress.getLoopbackAddress());
		server.registerHandler("/test", (LambdaPushContext request) -> {
			// Upgrade
			if (!request.getRequest().hasHeader("Upgrade"))
				return false;

			// Check headers
			if (!request.getRequest().hasHeader("Upgrade")
					|| !request.getRequest().getHeaderValue("Upgrade").equals("EDGEBINPROT/MMOUPLINK")) {
				request.setResponseStatus(400, "Bad request");
				return true;
			}

			// Set headers
			request.setResponseHeader("X-Response-ID", UUID.randomUUID().toString());
			request.setResponseHeader("Upgrade", "EDGEBINPROT/MMOUPLINK");

			// Setup
			AsyncTaskManager.runAsync(() -> {
				// Wait for upgrade
				while (request.getClient().isConnected()) {
					// Check
					if (request.getResponse().hasHeader("Upgraded")
							&& request.getResponse().getHeaderValue("Upgraded").equalsIgnoreCase("true"))
						break;

					// Wait
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						break;
					}
				}

				// Check
				if (request.getClient().isConnected()) {
					// Create client
					SimpleBinaryMessageClient client = new SimpleBinaryMessageClient((packet, cl) -> {
						return true;
					}, request.getClient().getInputStream(), request.getClient().getOutputStream());

					// Start
					client.start();

					// Stop client connection
					request.getClient().closeConnection();
				}
			});

			// Send response
			request.setResponseStatus(101, "Switching Protocols");
			return true;
		}, false, true);
		server.registerHandler("/test", (LambdaRequestContext request) -> {
			// Note: LambdaRequestContext only handles requests WITHOUT a request body
			request.setResponseStatus(200, "OK");
			request.setResponseContent("text/html", "<!DOCTYPE html><html><h1>Hello World</h1></html>");
		});
		server.registerHandler("/test", (LambdaPushContext request) -> {
			// This request handler also does request bodies
			String content = request.getRequestBodyAsString();
			request.setResponseStatus(200, "OK");
			request.setResponseContent(content);
		});
		server.start();
		server.waitForExit();
	}

	public static class SimpleBinaryMessageClient {

		private BiFunction<Packet, SimpleBinaryMessageClient, Boolean> handler;

		public Object container;

		private InputStream input;
		private OutputStream output;

		private long lastSent;
		private Object packetLock = new Object();
		private boolean connected;

		public static class Packet {
			public byte type;
			public byte[] data;
		}

		public SimpleBinaryMessageClient(BiFunction<Packet, SimpleBinaryMessageClient, Boolean> handler,
				InputStream input, OutputStream output) {
			this.handler = handler;
			this.input = input;
			this.output = output;
		}

		public void send(byte type, byte[] packet) throws IOException {
			if (type < 2)
				throw new IllegalArgumentException("Invalid packet type, must be above or equal to 2");
			sendPacketInternal(type, packet);
		}

		public void send(byte[] packet) throws IOException {
			sendPacketInternal((byte) 2, packet);
		}

		private void sendPacketInternal(byte type, byte[] packet) throws IOException {
			synchronized (packetLock) {
				try {
					// Write type
					output.write(type);
					if (type >= 2) {
						// Write length
						output.write(ByteBuffer.allocate(4).putInt(packet.length).array());
						output.write(packet);
					}
					lastSent = System.currentTimeMillis();
				} catch (IOException e) {
					if (connected)
						stop();
					throw e;
				}
			}
		}

		public void stop() {
			connected = false;
			try {
				// Send disconnect
				sendPacketInternal((byte) 0, new byte[0]);
			} catch (IOException e) {
			}

			// Disconnect streams
			try {
				input.close();
			} catch (IOException e) {
			}
			try {
				output.close();
			} catch (IOException e) {
			}
		}

		public boolean isConnected() {
			return connected;
		}

		public void startAsync() {
			// Start handling
			lastSent = System.currentTimeMillis();
			connected = true;
			AsyncTaskManager.runAsync(() -> start());
		}

		public void start() {
			// Start handling
			lastSent = System.currentTimeMillis();
			connected = true;

			// Start pinger
			AsyncTaskManager.runAsync(() -> {
				while (connected) {
					if ((System.currentTimeMillis() - lastSent) >= 5000) {
						// Ping
						try {
							sendPacketInternal((byte) 1, new byte[0]);
						} catch (IOException e) {
						}
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
				}
			});

			// Start reader
			try {
				while (connected) {
					int type = input.read();
					if (type == -1)
						break;
					switch (type) {

					// Disconnect
					case 0: {
						try {
							input.close();
						} catch (IOException e) {
						}
						try {
							output.close();
						} catch (IOException e) {
						}
						connected = false;
						break;
					}

					// Ping
					case 1: {
						break;
					}

					// Payload
					default: {
						// Read
						byte[] l = IoUtil.readNBytes(input, 4);
						if (l.length < 4)
							break;
						int length = ByteBuffer.wrap(l).getInt();

						// Handle
						Packet pk = new Packet();
						pk.type = (byte) type;
						pk.data = IoUtil.readNBytes(input, length);
						if (!handler.apply(pk, this))
							stop();
						break;
					}

					}
				}
			} catch (IOException e) {
			}
			connected = false;
		}

	}

}
