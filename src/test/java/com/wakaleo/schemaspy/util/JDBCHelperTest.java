/*
 * JDBCHelperTest.java
 * JUnit based test
 *
 * Created on 18 May 2007, 14:15
 */

package com.wakaleo.schemaspy.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * 
 * @author john
 */
public class JDBCHelperTest  {



    private final String[][] DATABASE_TYPES_TEST_DATA = {
            { "jdbc:derby:testdb", "derby" },
            { "jdbc:db2:testdb", "db2" },
            { "jdbc:firebirdsql://localhost/testdb", "firebirdsql" },
            { "jdbc:firebirdsql://server:9999/testdb", "firebirdsql" },
            { "jdbc:hsqldb:hsql://localhost/testdb", "hsqldb" },
            { "jdbc:hsqldb:hsql://server:9999/testdb", "hsqldb" },
            { "jdbc:informix-sqli://localhost/testdb:INFORMIXSERVER=server",
                    "informix-sqli" },
            { "jdbc:microsoft:sqlserver://server:9999;databaseName=testdb",
                    "mssql" },
            { "jdbc:jtds://server:9999/testdb", "mssql-jtds" },
            { "jdbc:mysql://localhost/testdb", "mysql" },
            { "jdbc:oracle:oci8:@testdb", "ora" },
            { "jdbc:oracle:thin:@server:9999:testdb", "orathin" },
            { "jdbc:postgresql://localhost/testdb", "pgsql" },
            { "jdbc:sybase:Tds:server:9999/testdb", "sybase" } };

    /**
     * Test of extractDatabaseType method, of class
     * com.wakaleo.maven.plugin.schemaspy.util.JDBCHelper.
     */
    @Test
    public void testExtractDatabaseType() {
        System.out.println("extractDatabaseType");

        JDBCHelper instance = new JDBCHelper();

        for (String[] testDataEntry : DATABASE_TYPES_TEST_DATA) {
            String jdbcUrl = testDataEntry[0];
            String expectedDatabaseType = testDataEntry[1];
            String result = instance.extractDatabaseType(jdbcUrl);
            assertEquals(expectedDatabaseType, result);
        }
    }

}
