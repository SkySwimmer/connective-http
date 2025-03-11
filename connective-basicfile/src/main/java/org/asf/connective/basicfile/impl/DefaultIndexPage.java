package org.asf.connective.basicfile.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.asf.connective.RemoteClient;
import org.asf.connective.basicfile.providers.IndexPageProvider;
import org.asf.connective.io.IoUtil;

public class DefaultIndexPage extends IndexPageProvider {

	@Override
	protected IndexPageProvider createInstance() {
		return new DefaultIndexPage();
	}

	@Override
	public void process(String path, String method, RemoteClient client, File[] files, File[] directories)
			throws IOException {
		try {
			// Read template index page
			InputStream strm = getClass().getResource("/index.template.html").openStream();

			// Process and set body
			setResponseContent("text/html", process(new String(IoUtil.readAllBytes(strm), "UTF-8"),
					getRequest().getRequestPath(), new File(getFolderPath()).getName(), null, directories, files));
			strm.close();
		} catch (IOException e) {
		}
	}

	private String process(String str, String path, String name, File data, File[] directories, File[] files) {
		// Remove windows line separators
		str = str.replace("\r", "");

		// Clean path a bit
		if (!path.endsWith("/")) {
			path += "/";
		}

		// If this is a entry process call, set the path and name fields
		if (data != null) {
			str = str.replace("%c-name%", name);
			str = str.replace("%c-path%", path);
		}

		// Process files and directories
		if (files != null) {
			// Parse NOTROOT block (only adds the specified entry if its not the root page
			if (str.contains("<%%PROCESS:NOTROOT:$")) {
				String buffer = "";
				String template = "";
				int percent = 0;
				boolean parsing = false;

				// Parse the block
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
								if (!buffer.equals("<%%PROCESS:NOTROOT:$")) {
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
								// Finish parsing
								percent = 0;
								template = buffer;

								// Clean buffer
								buffer = buffer.substring("<%%PROCESS:NOTROOT:$".length() + "\n".length(),
										buffer.length() - 4);

								// Replace template block with result
								StringBuilder strs = new StringBuilder();

								// Check if this is the root page
								if (!path.equals("/") && !path.isEmpty()) {
									// Not in the root page, add the block
									strs.append(buffer);
								}

								// Replace template from the content and clear buffer
								str = str.replace(template, strs.toString());
								buffer = "";
								parsing = false;
							} else {
								percent = 0;
							}
						}
					}
				}
			}

			// Parse file block
			if (str.contains("<%%PROCESS:FILES:$")) {
				String buffer = "";
				String template = "";
				int percent = 0;
				boolean parsing = false;

				// Parse the block
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
								// Finish parsing
								percent = 0;
								template = buffer;

								// Clean the buffer
								buffer = buffer.substring("<%%PROCESS:FILES:$".length() + "\n".length(),
										buffer.length() - 4);

								// Replace template block with result
								StringBuilder strs = new StringBuilder();
								for (File f : files) {
									// Use the template to add an entry to the result
									strs.append(process(buffer, path, f.getName(), f, null, null));
								}

								// Replace template from the content and clear buffer
								str = str.replace(template, strs.toString());
								buffer = "";
								parsing = false;
							} else {
								percent = 0;
							}
						}
					}
				}
			}

			// Parse directory blocks
			if (str.contains("<%%PROCESS:DIRECTORIES:$")) {
				String buffer = "";
				String template = "";
				int percent = 0;
				boolean parsing = false;

				// Parse the block
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
								// Finish parsing
								percent = 0;
								template = buffer;

								// Clean the buffer
								buffer = buffer.substring("<%%PROCESS:DIRECTORIES:$".length() + "\n".length(),
										buffer.length() - 4);

								// Replace template block with the result
								StringBuilder strs = new StringBuilder();
								for (File f : directories) {
									strs.append(process(buffer, path, f.getName(), f, null, null));
								}

								// Replace template from the content and clear buffer
								str = str.replace(template, strs.toString());
								buffer = "";
								parsing = false;
							} else {
								percent = 0;
							}
						}
					}
				}
			}
		}

		// Create a pretty path string
		String prettyPath = path;
		if (prettyPath.endsWith("/") && !prettyPath.equals("/"))
			prettyPath = prettyPath.substring(0, prettyPath.length() - 1);
		if (!prettyPath.equals("/"))
			prettyPath = prettyPath.substring(1);

		// Replace rest of template data
		str = str.replace("%path%", path);
		str = str.replace("%path-pretty%", prettyPath);
		str = str.replace("%name%", name);
		str = str.replace("%up-path%", (path.equals("/") || path.isEmpty()) ? "" : new File(path).getParent());
		str = str.replace("%server-name%", getServer().getServerName());
		str = str.replace("%server-version%", getServer().getServerVersion());

		return str;
	}

}
