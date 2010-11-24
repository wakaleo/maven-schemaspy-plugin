package com.wakaleo.schemaspy;

import com.wakaleo.schemaspy.util.DatabaseHelper;
import java.io.File;
import java.util.Locale;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

/**
 * SchemaSpyReport unit tests Test POM files is kept in test/resources/unit
 * directory.
 * 
 * @author john
 */
public class SchemaSpyMojoTest extends AbstractMojoTestCase {

    public SchemaSpyMojoTest() {
    }

    protected void setUp() throws Exception {
        super.setUp();
        DatabaseHelper.setupDatabase("src/test/resources/sql/testdb.sql");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCustomConfiguration() throws Exception {

        File testPom = new File(getBasedir(),
                "src/test/resources/unit/test-plugin-config.xml");

        SchemaSpyReport mojo = (SchemaSpyReport) lookupMojo("schemaspy",
                testPom);
        mojo.executeReport(Locale.getDefault());

        // check if the reports generated
        File generatedFile = new File("./target/reports/test/schemaspy/index.html");
        System.out
                .println("generatedFile = " + generatedFile.getAbsolutePath());
        assertTrue(generatedFile.exists());
    }

    public void testFullConfiguration() throws Exception {

        File testPom = new File(getBasedir(),
                "src/test/resources/unit/full-test-plugin-config.xml");

        SchemaSpyReport mojo = (SchemaSpyReport) lookupMojo("schemaspy",
                testPom);
        mojo.executeReport(Locale.getDefault());

        // check if the reports generated
        File generatedFile = new File("./target/reports/test/schemaspy/index.html");
        System.out
                .println("generatedFile = " + generatedFile.getAbsolutePath());
        assertTrue(generatedFile.exists());

    }

    public void testConfigurationUsingJDBCUrl() throws Exception {

        File testPom = new File(getBasedir(),
                "src/test/resources/unit/jdbcurl-test-plugin-config.xml");

        SchemaSpyReport mojo = (SchemaSpyReport) lookupMojo("schemaspy",
                testPom);
        mojo.executeReport(Locale.getDefault());

        // check if the reports generated
        File generatedFile = new File("./target/reports/test/schemaspy/index.html");
        System.out
                .println("generatedFile = " + generatedFile.getAbsolutePath());
        assertTrue(generatedFile.exists());
        
    }

}
