package org.asf.connective.standalone.configuration;

import java.util.Map;

public class PropertyBridge {

	public Map<String, String> properties;

	public PropertyBridge(Map<String, String> properties) {
		this.properties = properties;
	}

	public void Set(String key, Object value) {
		properties.put(key, value.toString());
	}

}
