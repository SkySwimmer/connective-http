package org.asf.connective.testserver.testhandlers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

public class MainFileMap extends MimetypesFileTypeMap {
	private static MainFileMap instance;

	private FileTypeMap parent;

	public static MainFileMap getInstance() {
		if (instance == null) {
			instance = new MainFileMap(MimetypesFileTypeMap.getDefaultFileTypeMap());
		}
		return instance;
	}

	public MainFileMap(FileTypeMap parent) {
		this.parent = parent;
		this.addMimeTypes("application/xml	xml");
		this.addMimeTypes("application/json	json");
		this.addMimeTypes("text/ini	ini	ini");
		this.addMimeTypes("text/css	css");
		this.addMimeTypes("text/javascript	js");
		if (new File(".mime.types").exists()) {
			try {
				this.addMimeTypes(new String(Files.readAllBytes(new File(".mime.types").toPath()), "UTF-8"));
			} catch (IOException e) {
			}
		}
		if (new File("mime.types").exists()) {
			try {
				this.addMimeTypes(new String(Files.readAllBytes(new File("mime.types").toPath()), "UTF-8"));
			} catch (IOException e) {
			}
		}
	}

	@Override
	public String getContentType(String filename) {
		String type = super.getContentType(filename);
		if (type.equals("application/octet-stream")) {
			type = parent.getContentType(filename);
		}
		return type;
	}
}
