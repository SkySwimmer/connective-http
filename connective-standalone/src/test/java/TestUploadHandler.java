import java.io.File;
import java.io.IOException;

import org.asf.connective.RemoteClient;
import org.asf.connective.basicfile.providers.FileUploadHandlerProvider;
import org.asf.connective.objects.HttpRequest;

public class TestUploadHandler extends FileUploadHandlerProvider {

	public String test;
	public String test2;

	public TestUploadHandler(String test, String test2) {
		this.test = test;
		this.test2 = test2;
	}

	@Override
	protected FileUploadHandlerProvider createInstance() {
		return new TestUploadHandler(test, test2);
	}

	@Override
	public boolean match(HttpRequest request, String inputPath, String method) {
		return inputPath.equals("/abc");
	}

	@Override
	public void process(File file, String path, String method, RemoteClient client, String contentType)
			throws IOException {
		file = file;
	}

}
