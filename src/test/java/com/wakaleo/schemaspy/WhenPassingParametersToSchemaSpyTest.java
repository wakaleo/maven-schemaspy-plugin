package com.wakaleo.schemaspy;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class WhenPassingParametersToSchemaSpyTest extends AbstractMojoTestCase {

	public void testThePathToDriversOptionIsPassedAsDP() throws Exception {
        File testPom = new File(getBasedir(), "src/test/projects/unit/oracle-plugin-config.xml");
        SchemaSpyReport mojo = (SchemaSpyReport) lookupMojo("schemaspy", testPom);
    	MockSchemaAnalyzer analyzer = new MockSchemaAnalyzer();
        mojo.setSchemaAnalyzer(analyzer);
        
        mojo.executeReport(Locale.getDefault());
        
        assertThat(analyzer.getConfig(), is(notNullValue()));
        assertThat(analyzer.getConfig().getDriverPath(), containsString("oracle"));
		
	}
}
