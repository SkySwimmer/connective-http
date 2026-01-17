package org.asf.connective.testserver.testhandlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.asf.connective.RemoteClient;
import org.asf.connective.handlers.HttpRequestHandler;
import org.asf.connective.io.IoUtil;

public class TestRequestProcessor extends HttpRequestHandler {

	@Override
	public HttpRequestHandler createNewInstance() {
		return new TestRequestProcessor();
	}

	@Override
	public String path() {
		return "/";
	}

	@Override
	public boolean supportsChildPaths() {
		return true;
	}

	@Override
	public void handle(String path, String method, RemoteClient client) {
		// Find file
		File sourceFile = new File("root", path);
		if (sourceFile.exists()) {
			// Send file
			if (!sourceFile.isDirectory()) {
				try {
					InputStream strm = new FileInputStream(sourceFile);
					setResponseContent(MainFileMap.getInstance().getContentType(sourceFile.getName()), strm);
					setResponseStatus(200, "OK");
				} catch (FileNotFoundException e) {
					setResponseStatus(404, "Not found");
				}
			} else {
				try {
					InputStream strmI = getClass().getResource("/index.template.html").openStream();
					setResponseContent("text/html",
							process(new String(IoUtil.readAllBytes(strmI)), getRequestPath(), sourceFile.getName(),
									null, sourceFile.listFiles(t -> t.isDirectory()),
									sourceFile.listFiles(t -> !t.isDirectory())));
					strmI.close();
				} catch (IOException e) {
					setResponseStatus(404, "Not found");
				}
			}
		} else
			setResponseStatus(404, "Not found");
	}

	private String process(String str, String path, String name, File data, File[] directories, File[] files) {
		if (!path.endsWith("/")) {
			path += "/";
		}

		if (data != null) {
			str = str.replace("%c-name%", name);
			str = str.replace("%c-path%", path);
		}

		if (files != null) {
			if (str.contains("<%%PROCESS:FILES:$")) {
				String buffer = "";
				String template = "";
				int percent = 0;
				boolean parsing = false;
				for (char ch : str.toCharArray()) {
					if (ch == '<' && !parsing) {
						if (buffer.isEmpty()) {
							buffer = "<";
						} else {
							buffer = "";
						}
					} else if (ch == '\n' && !parsing) {
						buffer = "";
					} else if (ch == '\n') {
						buffer += "\n";
					} else {
						if (!buffer.isEmpty() && !parsing) {
							buffer += ch;
							if (ch == '$') {
								if (!buffer.equals("<%%PROCESS:FILES:$")) {
									buffer = "";
								} else {
									parsing = true;
								}
							}
						} else if (parsing) {
							buffer += ch;
							if (ch == '%' && percent < 2) {
								percent++;
							} else if (ch == '%' && percent >= 2) {
								percent = 0;
							} else if (ch == '>' && percent == 2) {
								percent = 0;
								template = buffer;
								buffer = buffer.substring(
										"<%%PROCESS:FILES:$".length() + System.lineSeparator().length(),
										buffer.length() - 4);

								StringBuilder strs = new StringBuilder();
								for (File f : files) {
									strs.append(process(buffer, path, f.getName(), f, null, null));
								}
								str = str.replace(template, strs.toString());
								buffer = "";
							} else {
								percent = 0;
							}
						}
					}
				}
			}
			if (str.contains("<%%PROCESS:DIRECTORIES:$")) {
				String buffer = "";
				String template = "";
				int percent = 0;
				boolean parsing = false;
				for (char ch : str.toCharArray()) {
					if (ch == '<' && !parsing) {
						if (buffer.isEmpty()) {
							buffer = "<";
						} else {
							buffer = "";
						}
					} else if (ch == '\n' && !parsing) {
						buffer = "";
					} else if (ch == '\n') {
						buffer += "\n";
					} else {
						if (!buffer.isEmpty() && !parsing) {
							buffer += ch;
							if (ch == '$') {
								if (!buffer.equals("<%%PROCESS:DIRECTORIES:$")) {
									buffer = "";
								} else {
									parsing = true;
								}
							}
						} else if (parsing) {
							buffer += ch;
							if (ch == '%' && percent < 2) {
								percent++;
							} else if (ch == '%' && percent >= 2) {
								percent = 0;
							} else if (ch == '>' && percent == 2) {
								percent = 0;
								template = buffer;
								buffer = buffer.substring(
										"<%%PROCESS:DIRECTORIES:$".length() + System.lineSeparator().length(),
										buffer.length() - 4);

								StringBuilder strs = new StringBuilder();
								for (File f : directories) {
									strs.append(process(buffer, path, f.getName(), f, null, null));
								}
								str = str.replace(template, strs.toString());
								buffer = "";
							} else {
								percent = 0;
							}
						}
					}
				}
			}
		}

		String prettyPath = path;
		if (prettyPath.endsWith("/") && !prettyPath.equals("/"))
			prettyPath = prettyPath.substring(0, prettyPath.length() - 1);

		str = str.replace("%path%", path);
		str = str.replace("%path-pretty%", prettyPath);
		str = str.replace("%name%", name);
		str = str.replace("%up-path%", (path.equals("/") || path.isEmpty()) ? "" : new File(path).getParent());
		str = str.replace("%server-name%", getServer().getServerName());
		str = str.replace("%server-version%", getServer().getServerVersion());

		return str;
	}

}
