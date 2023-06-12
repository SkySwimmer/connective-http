package org.asf.connective.standalone.impl;

import org.asf.connective.standalone.modules.IMavenRepositoryProvider;

public class MavenCentralRepositoryProvider implements IMavenRepositoryProvider {

	@Override
	public String serverBaseURL() {
		return "https://repo1.maven.org/maven2";
	}

}
