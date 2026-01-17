package org.asf.connective;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.asf.connective.handlers.*;
import org.asf.connective.io.IoUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Random;

public class ConnectiveServerTest {

	Random rnd = new Random();

	public String genText() {
		char[] chars = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
				'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
		String txt = "";

		for (int i = 0; i < rnd.nextInt(100000); i++) {
			txt += chars[rnd.nextInt(chars.length)];
		}
		return txt;
	}

	class TestProc extends HttpPushHandler {

		@Override
		public String path() {
			return "/test";
		}

		@Override
		public void handle(String path, String method, RemoteClient client, String contentType) throws IOException {
			getResponse().addHeader("Connection", "Keep-Alive");
			if (contentType != null) {
				byte[] data = IoUtil.readAllBytes(getRequest().getBodyStream());
				setResponseContent(new String(data, "UTF-8") + "-test");
			} else {
				setResponseContent("12345");
			}
		}

		@Override
		public HttpPushHandler createNewInstance() {
			return new TestProc();
		}

		@Override
		public boolean supportsNonPush() {
			return true;
		}

	}

	@Test
	public void keepAliveConnectionTest() throws IOException {
		NetworkedConnectiveHttpServer testServer = ConnectiveHttpServer.createNetworked("HTTP/1.1");
		testServer.setListenPort(12345);
		testServer.start();
		testServer.registerHandler(new TestProc());

		URL u = new URL("http://localhost:" + testServer.getListenPort() + "/test?test=hi&test2=hello");
		InputStream strm = u.openStream();
		byte[] test = IoUtil.readAllBytes(strm);
		strm.close();
		String outp = new String(test);

		assertTrue(outp.equals("12345"));

		// Now test keep-alive
		Socket sock = new Socket("localhost", testServer.getListenPort());

		// Write first request
		sock.getOutputStream().write("GET /test HTTP/1.1\r\n".getBytes("UTF-8"));
		sock.getOutputStream().write("Host: localhost\r\n".getBytes("UTF-8"));
		sock.getOutputStream().write("\r\n".getBytes("UTF-8"));

		// Read response
		HashMap<String, String> headers = new HashMap<String, String>();
		String line = readStreamLine(sock.getInputStream());
		assertTrue(line.equals("HTTP/1.1 200 OK"));
		while (true) {
			line = readStreamLine(sock.getInputStream());
			if (line.equals(""))
				break;
			String key = line.substring(0, line.indexOf(": "));
			String value = line.substring(line.indexOf(": ") + 2);
			headers.put(key, value);
		}
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		transferRequestBody(headers, sock.getInputStream(), buffer);
		String data = new String(buffer.toByteArray(), "UTF-8");
		assertTrue(data.equals("12345"));

		// Send second request
		sock.getOutputStream().write("GET /test HTTP/1.1\r\n".getBytes("UTF-8"));
		sock.getOutputStream().write("Host: localhost\r\n".getBytes("UTF-8"));
		sock.getOutputStream().write("\r\n".getBytes("UTF-8"));

		// Read response
		headers.clear();
		line = readStreamLine(sock.getInputStream());
		assertTrue(line.equals("HTTP/1.1 200 OK"));
		while (true) {
			line = readStreamLine(sock.getInputStream());
			if (line.equals(""))
				break;
			String key = line.substring(0, line.indexOf(": "));
			String value = line.substring(line.indexOf(": ") + 2);
			headers.put(key, value);
		}
		buffer = new ByteArrayOutputStream();
		transferRequestBody(headers, sock.getInputStream(), buffer);
		data = new String(buffer.toByteArray(), "UTF-8");
		assertTrue(data.equals("12345"));

		// Send third request
		sock.getOutputStream().write("POST /test HTTP/1.1\r\n".getBytes("UTF-8"));
		sock.getOutputStream().write("Host: localhost\r\n".getBytes("UTF-8"));
		sock.getOutputStream().write("Content-Type: text/plain\r\n".getBytes("UTF-8"));
		sock.getOutputStream().write("Content-Length: 4\r\n".getBytes("UTF-8"));
		sock.getOutputStream().write("\r\n".getBytes("UTF-8"));
		sock.getOutputStream().write("Test".getBytes("UTF-8"));

		// Read response
		headers.clear();
		line = readStreamLine(sock.getInputStream());
		assertTrue(line.equals("HTTP/1.1 200 OK"));
		while (true) {
			line = readStreamLine(sock.getInputStream());
			if (line.equals(""))
				break;
			String key = line.substring(0, line.indexOf(": "));
			String value = line.substring(line.indexOf(": ") + 2);
			headers.put(key, value);
		}
		buffer = new ByteArrayOutputStream();
		transferRequestBody(headers, sock.getInputStream(), buffer);
		data = new String(buffer.toByteArray(), "UTF-8");
		assertTrue(data.equals("Test-test"));

		sock.close();
		testServer.stop();
	}

	@Test
	public void getTest() throws IOException {
		NetworkedConnectiveHttpServer testServer = ConnectiveHttpServer.createNetworked("HTTP/1.1");
		testServer.setListenPort(12345);
		testServer.start();
		testServer.registerHandler(new TestProc());

		URL u = new URL("http://localhost:" + testServer.getListenPort() + "/test?test=hi&test2=hello");
		InputStream strm = u.openStream();
		byte[] test = IoUtil.readAllBytes(strm);
		strm.close();
		String outp = new String(test);

		assertTrue(outp.equals("12345"));
		testServer.stop();
	}

	@Test
	public void malformedTest() throws IOException {
		NetworkedConnectiveHttpServer testServer = ConnectiveHttpServer.createNetworked("HTTP/1.1");
		testServer.setListenPort(12345);
		testServer.start();
		testServer.registerHandler(new TestProc());

		URL u = new URL("http://localhost:" + testServer.getListenPort() + "/%YE");
		HttpURLConnection conn = (HttpURLConnection) u.openConnection();
		assertTrue(conn.getResponseCode() == 400);

		u = new URL("http://localhost:" + testServer.getListenPort() + "/test?malformed=%YE");
		conn = (HttpURLConnection) u.openConnection();
		int code = conn.getResponseCode();
		assertTrue(code != 500);
	}

	@Test
	public void postTest() throws IOException {
		NetworkedConnectiveHttpServer testServer = ConnectiveHttpServer.createNetworked("HTTP/1.1");
		testServer.setListenPort(12345);
		testServer.registerHandler(new TestProc());
		String str = genText();
		testServer.start();

		URL u = new URL("http://localhost:" + testServer.getListenPort() + "/test");
		HttpURLConnection c = (HttpURLConnection) u.openConnection();
		try {
			c.setRequestMethod("POST");
			c.setDoOutput(true);
			c.setDoInput(true);
			c.connect();
			c.getOutputStream().write(str.getBytes());

			int code = c.getResponseCode();
			String msg = c.getResponseMessage();
			if (code == 200) {
				String resp = new String(IoUtil.readAllBytes(c.getInputStream()));
				c.disconnect();
				assertTrue(resp.equals(str + "-test"));
			} else {
				c.disconnect();
				testServer.stop();
				fail(msg);
			}
		} catch (IOException e) {
			c.disconnect();
			testServer.stop();
			throw e;
		}

		testServer.stop();
	}

	static String readStreamLine(InputStream strm) throws IOException {
		String buffer = "";
		while (true) {
			char ch = (char) strm.read();
			if (ch == (char) -1)
				return null;
			if (ch == '\n') {
				return buffer;
			} else if (ch != '\r') {
				buffer += ch;
			}
		}
	}

	static void transferRequestBody(HashMap<String, String> headers, InputStream bodyStream, OutputStream output)
			throws IOException {
		if (headers.containsKey("Content-Length")) {
			long length = Long.valueOf(headers.get("Content-Length"));
			int tr = 0;
			for (long i = 0; i < length; i += tr) {
				tr = Integer.MAX_VALUE / 1000;
				if ((length - (long) i) < tr) {
					tr = bodyStream.available();
					if (tr == 0) {
						output.write(bodyStream.read());
						i += 1;
					}
					tr = bodyStream.available();
				}
				output.write(IoUtil.readNBytes(bodyStream, tr));
			}
		} else {
			IoUtil.transfer(bodyStream, output);
		}
	}
}
