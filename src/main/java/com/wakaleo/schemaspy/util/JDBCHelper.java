/*
 * JDBCHelper.java
 *
 * Created on 18 May 2007, 14:12
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.wakaleo.schemaspy.util;

/**
 * A utilities class for the SchemaSpy plugin. Tries to guess the right
 * SchemaSpy database type configuration value using a JDBC URL.
 *
 * @author john
 */
public class JDBCHelper {

    /** Creates a new instance of JDBCHelper. */
    public JDBCHelper() {
    }

    /**
     * A map of JDBC prefixes and database types.
     * Used to determine the database type based on a JDBC URL.
     */
    private static final String[][] DATABASE_TYPES_MAP
          = {{"jdbc:derby:", "derby" },
            {"jdbc:db2:", "db2"},
            {"jdbc:firebirdsql:", "firebirdsql"},
            {"jdbc:hsqldb:hsql:", "hsqldb" },
            {"jdbc:informix-sqli:", "informix-sqli" },
            {"jdbc:microsoft:sqlserver:", "mssql" },
            {"jdbc:jtds:", "mssql-jtds" },
            {"jdbc:mysql:", "mysql" },
            {"jdbc:oracle:oci8:", "ora" },
            {"jdbc:oracle:thin:", "orathin" },
            {"jdbc:postgresql:", "pgsql" },
            {"jdbc:sybase:Tds:", "sybase" }};

    /**
     * Find the matching SchemaSpy database type from a JDBC URL.
     * If none match, <code>null</code> is returned.
     * @param jdbcUrl a valid JDBC url for the target database
     * @return the type of the database for this JDBC URL
     */
    public final String extractDatabaseType(final String jdbcUrl) {

        String result = null;
        for (String[] databaseTypeEntry : DATABASE_TYPES_MAP) {
            String jdbcPrefix = databaseTypeEntry[0];
            if (jdbcUrl.startsWith(jdbcPrefix)) {
                result = databaseTypeEntry[1];
            }
        }
        return result;
    }
}
