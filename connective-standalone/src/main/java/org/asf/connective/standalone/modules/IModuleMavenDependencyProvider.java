package org.asf.connective.standalone.modules;

public interface IModuleMavenDependencyProvider {
	public String group();

	public String name();

	public String version();

	public default String classifier() {
		return null;
	}

	public default String extension() {
		return ".jar";
	}
}
