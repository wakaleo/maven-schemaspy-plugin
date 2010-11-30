package com.wakaleo.schemaspy;

import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.SchemaAnalyzer;
import net.sourceforge.schemaspy.model.Database;

public class MockSchemaAnalyzer extends SchemaAnalyzer {

	private Config config;
	
	public Config getConfig() {
		return config;
	}
	
	@Override
	public Database analyze(Config config) throws Exception {
		this.config = config;
		return null;
	}
}
