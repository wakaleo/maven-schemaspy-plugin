package com.wakaleo.schemaspy;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.Locale;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * SchemaSpyReport unit tests Test POM files is kept in test/resources/unit
 * directory.
 * 
 * @author john
 */
public class MysqlExternalDatabaseTest {

    /**
     * Test resources.
     */
    @Rule
    public TestResources resources = new TestResources();

    /**
     * test rule.
     */
    @Rule
    public MojoRule rule = new MojoRule();

    @Test
    public void testMySqlConfiguration() throws Exception {

        File projectCopy = this.resources.getBasedir("unit");
        File testPom = new File(projectCopy,"mysql-plugin-config.xml");
        assumeNotNull("POM file should not be null.", testPom);
        assumeTrue("POM file should exist as file.",
                testPom.exists() && testPom.isFile());

        SchemaSpyReport mojo = (SchemaSpyReport)this.rule.lookupMojo("schemaspy", testPom);
        mojo.executeReport(Locale.getDefault());

        // check if the reports generated
        File generatedFile = new File("./target/reports/mysql-test/schemaspy/index.html");
        assertTrue(generatedFile.exists());
    }

}
