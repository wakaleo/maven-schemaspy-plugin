package com.wakaleo.schemaspy;

import com.wakaleo.schemaspy.util.DatabaseHelper;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Before;
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
public class SchemaSpyMojoTest  {

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

    @Before
    public void setUp() throws Exception {
        DatabaseHelper.setupDatabase("src/test/resources/sql/testdb.sql");
    }

    @Test
    public void testCustomConfiguration() throws Exception {
        File projectCopy = this.resources.getBasedir("unit");
        File testPom = new File(projectCopy,"test-plugin-config.xml");
        assumeNotNull("POM file should not be null.", testPom);
        assumeTrue("POM file should exist as file.",
                testPom.exists() && testPom.isFile());

        SchemaSpyReport mojo = (SchemaSpyReport) this.rule.lookupMojo("schemaspy",testPom);
        mojo.executeReport(Locale.getDefault());

        // check if the reports generated
        File generatedFile = new File("./target/reports/test/schemaspy/index.html");
        System.out.println("generatedFile = " + generatedFile.getAbsolutePath());
        assertTrue(generatedFile.exists());
    }

    @Test
    public void testFullConfiguration() throws Exception {
        File projectCopy = this.resources.getBasedir("unit");
        File testPom = new File(projectCopy,"full-test-plugin-config.xml");
        assumeNotNull("POM file should not be null.", testPom);
        assumeTrue("POM file should exist as file.",
                testPom.exists() && testPom.isFile());

        SchemaSpyReport mojo = (SchemaSpyReport) this.rule.lookupMojo("schemaspy",testPom);
        mojo.executeReport(Locale.getDefault());

        // check if the reports generated
        File generatedFile = new File("./target/reports/full-test/schemaspy/index.html");
        System.out.println("generatedFile = " + generatedFile.getAbsolutePath());
        assertTrue(generatedFile.exists());

    }
    @Test
    public void testConfigurationUsingJDBCUrl() throws Exception {
        File projectCopy = this.resources.getBasedir("unit");
        File testPom = new File(projectCopy,"jdbcurl-test-plugin-config.xml");
        assumeNotNull("POM file should not be null.", testPom);
        assumeTrue("POM file should exist as file.",
                testPom.exists() && testPom.isFile());

        SchemaSpyReport mojo = (SchemaSpyReport) this.rule.lookupMojo("schemaspy",testPom);
        mojo.executeReport(Locale.getDefault());

        // check if the reports generated
        File generatedFile = new File("./target/reports/jdbcurl-test/schemaspy/index.html");
        System.out.println("generatedFile = " + generatedFile.getAbsolutePath());
        assertTrue(generatedFile.exists());
    }

}
