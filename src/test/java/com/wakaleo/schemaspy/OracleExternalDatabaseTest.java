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
public class OracleExternalDatabaseTest extends AbstractMojoTestCase {

    public OracleExternalDatabaseTest() {
    }

    public void testMySqlConfiguration() throws Exception {

        File testPom = new File(getBasedir(),
                "src/test/resources/unit/oracle-plugin-config.xml");

        SchemaSpyReport mojo = (SchemaSpyReport) lookupMojo("schemaspy", testPom);
        mojo.executeReport(Locale.getDefault());

        // check if the reports generated
        File generatedFile = new File("./target/reports/oracle-test/schemaspy/index.html");
        assertTrue(generatedFile.exists());
    }

}
