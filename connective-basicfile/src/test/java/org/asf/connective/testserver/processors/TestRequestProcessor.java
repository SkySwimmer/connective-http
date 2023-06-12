package org.asf.connective.testserver.processors;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpRequestProcessor;
import org.asf.connective.basicfile.FileProviderContext;
import org.asf.connective.basicfile.FileProviderContextFactory;
import org.asf.connective.basicfile.providers.IndexPageProvider;

public class TestRequestProcessor extends HttpRequestProcessor {

	private static FileProviderContext ctx;
	static {
		FileProviderContextFactory fac = new FileProviderContextFactory();
		ctx = fac.build();
	}

	@Override
	public String path() {
		return "/";
	}

	@Override
	public HttpRequestProcessor createNewInstance() {
		return new TestRequestProcessor();
	}

	@Override
	public boolean supportsChildPaths() {
		return true;
	}

	@Override
	public void process(String path, String method, RemoteClient client) throws IOException {
		// Index page test
		File f = new File(".", getRequestPath());
		if (!f.exists()) {
			this.setResponseStatus(404, "Not found");
			return;
		}
		if (f.isDirectory()) {
			IndexPageProvider index = ctx.getIndexPage(path);
			if (index != null)
				index.instantiate(getServer(), getRequest(), getResponse(), f.listFiles(t -> t.isFile()),
						f.listFiles(t -> t.isDirectory()), getRequestPath())
						.process(path, method, client, f.listFiles(t -> t.isFile()), f.listFiles(t -> t.isDirectory()));
			else
				this.setResponseStatus(404, "Not found");
		} else
			setResponseContent(MainFileMap.getInstance().getContentType(f), new FileInputStream(f));
	}

}
