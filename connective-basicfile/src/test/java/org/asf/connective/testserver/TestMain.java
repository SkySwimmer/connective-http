package org.asf.connective.testserver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.NetworkedConnectiveHttpServer;
import org.asf.connective.RemoteClient;
import org.asf.connective.basicfile.FileProviderContext;
import org.asf.connective.basicfile.FileProviderContextFactory;
import org.asf.connective.basicfile.providers.FileUploadHandlerProvider;
import org.asf.connective.basicfile.providers.IFileAliasProvider;
import org.asf.connective.basicfile.providers.IFileRestrictionProvider;
import org.asf.connective.basicfile.providers.IVirtualFileProvider;
import org.asf.connective.basicfile.providers.extensions.IContextProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IRemoteClientProviderExtension;
import org.asf.connective.basicfile.providers.extensions.IServerProviderExtension;
import org.asf.connective.basicfile.providers.IFileExtensionProvider;
import org.asf.connective.basicfile.util.BasicfileContentSource;
import org.asf.connective.basicfile.util.FileContext;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.connective.testserver.CgiScript.CgiContext;
import org.asf.connective.testserver.processors.TestRequestProcessor;

public class TestMain {

	public static void main(String[] args) throws IOException {
		NetworkedConnectiveHttpServer server = ConnectiveHttpServer.createNetworked("HTTP/1.1");
		server.registerHandler(new TestRequestProcessor());

		// Create content source
		BasicfileContentSource source = new BasicfileContentSource();

		// Configure the context
		FileProviderContextFactory fac = new FileProviderContextFactory();
		fac.registerAlias(new TestAlias());
		fac.setFileSourceFolder("run/testdata");
		source.registerContext("/", fac.build());

		// Register the second context
		fac = new FileProviderContextFactory();
		fac.setFileSourceFolder("run/testdata/centraltest");
		fac.registerRestriction(new TestRestriction());
		fac.registerVirtualFile(new TestVirtualFile());
		fac.registerFileExtension(new TestFileExtension());
		fac.registerUploadHandler(new TestFileUploadHandler());
		source.registerContext("/localhost:8080/", fac.build());

		// Assign and start
		server.setContentSource(source);
		server.start();
		server.waitForExit();
	}

	public static class TestFileExtension implements IFileExtensionProvider, IContextProviderExtension,
			IServerProviderExtension, IRemoteClientProviderExtension {

		private ConnectiveHttpServer server;
		private RemoteClient client;
		private FileProviderContext context;

		@Override
		public IFileExtensionProvider createInstance() {
			return new TestFileExtension();
		}

		@Override
		public String fileExtension() {
			return ".php";
		}

		@Override
		public FileContext rewrite(String path, InputStream fileSource, HttpResponse input, HttpRequest request)
				throws IOException {
			// Run php
			CgiScript cgi = CgiScript.create(server, "/usr/bin/php-cgi");
			cgi.setDefaultVariables("test server", request, client);
			cgi.setVariable("REDIRECT_STATUS", "1");
			cgi.setVariable("PGP_SELF", path);
			cgi.setFileVariable(context, path);
			cgi.addContentProvider(request);
			CgiContext ctx = cgi.run();
			ctx.applyToResponse(request, input);
			return FileContext.create(input, input.getHeaderValue("Content-Type"), ctx.getOutput());
		}

		@Override
		public void provide(RemoteClient client) {
			this.client = client;
		}

		@Override
		public void provide(ConnectiveHttpServer server) {
			this.server = server;
		}

		@Override
		public void provide(FileProviderContext context) {
			this.context = context;
		}

	}

	public static class TestFileUploadHandler extends FileUploadHandlerProvider {

		@Override
		protected FileUploadHandlerProvider createInstance() {
			return new TestFileUploadHandler();
		}

		@Override
		public boolean match(HttpRequest request, String inputPath, String method) {
			return inputPath.equals("/secure/test123") || inputPath.startsWith("/secure/test123/");
		}

		@Override
		public void process(File file, String path, String method, RemoteClient client, String contentType)
				throws IOException {
			// Check method
			if (method.equals("DELETE")) {
				// Delete
				file.delete();
				setResponseStatus(200, "OK");
			} else {
				// Write file
				boolean existed = file.exists();
				file.getParentFile().mkdirs();
				FileOutputStream strOut = new FileOutputStream(file);
				getRequest().transferRequestBody(strOut);
				strOut.close();
				if (!existed)
					setResponseStatus(201, "Created");
				else
					setResponseStatus(200, "OK");
			}
		}

	}

	public static class TestVirtualFile implements IVirtualFileProvider {

		@Override
		public IVirtualFileProvider createInstance() {
			return new TestVirtualFile();
		}

		@Override
		public boolean match(HttpRequest request, String path) {
			return path.equalsIgnoreCase("/testfile");
		}

		@Override
		public void process(String method, HttpRequest request, HttpResponse response, String path,
				String uploadMediaType, RemoteClient client) {
			response.setContent("text/plain", "hello world");
		}

	}

	public static class TestRestriction implements IFileRestrictionProvider {

		@Override
		public IFileRestrictionProvider createInstance() {
			return new TestRestriction();
		}

		@Override
		public boolean match(HttpRequest request, String inputPath) {
			return (inputPath + "/").toLowerCase().startsWith("/secure/");
		}

		@Override
		public boolean checkRestriction(String file, HttpRequest request, HttpResponse response) {
			if (request.hasHeader("Authorization")) {
				// Check auth
				String auth = request.getHeaderValue("Authorization");
				if (auth.startsWith("Basic ")) {
					String cred = new String(Base64.getDecoder().decode(auth.substring("Basic ".length())));
					return cred.equals("test:test123");
				}
			}
			return false;
		}

		@Override
		public int getResponseCode(HttpRequest request) {
			return 401;
		}

		@Override
		public String getResponseMessage(HttpRequest request) {
			return "Unauthorized";
		}

		@Override
		public void rewriteResponse(HttpRequest request, HttpResponse response) {
			response.addHeader("WWW-Authenticate", "Basic realm=test");
		}

	}

	public static class TestAlias implements IFileAliasProvider {

		@Override
		public IFileAliasProvider createInstance() {
			return new TestAlias();
		}

		@Override
		public boolean match(HttpRequest request, String inputPath) {
			return true;
		}

		@Override
		public String applyAlias(HttpRequest request, String inputPath) {
			// Alias it by adding the host to the path
			if (request.hasHeader("Host"))
				return "/" + request.getHeaderValue("Host") + "/" + inputPath;
			else
				return "/central/" + inputPath;
		}

	}

}
