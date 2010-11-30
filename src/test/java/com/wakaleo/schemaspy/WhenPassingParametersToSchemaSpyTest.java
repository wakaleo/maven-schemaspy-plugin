package com.wakaleo.schemaspy;

import java.io.File;
import java.util.Locale;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;
import net.sourceforge.schemaspy.SchemaAnalyzer;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Test;

public class WhenPassingParametersToSchemaSpyTest extends AbstractMojoTestCase {

	public void testThePathToDriversOptionIsPassedAsDP() throws Exception {
        File testPom = new File(getBasedir(), "src/test/resources/unit/oracle-plugin-config.xml");
        SchemaSpyReport mojo = (SchemaSpyReport) lookupMojo("schemaspy", testPom);
    	MockSchemaAnalyzer analyzer = new MockSchemaAnalyzer();
        mojo.setSchemaAnalyzer(analyzer);
        
        mojo.executeReport(Locale.getDefault());
        
        assertThat(analyzer.getConfig(), is(notNullValue()));
        assertThat(analyzer.getConfig().getDriverPath(), containsString("oracle"));
		
	}
}
