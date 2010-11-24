package com.wakaleo.schemaspy.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Set up a simple embedded database to run SchemaSpy on. We use a Derby
 * database.
 * 
 * @author john
 */
public class DatabaseHelper {

    private static Boolean databaseSetupDone = false;

    /**
     * Create an embedded Derby database using a simple SQL script. The SQL
     * commands should each be on a single line. "--" and "//" can be used for
     * comments.
     * 
     * @param sqlCreateScript
     *            Path to a file containing the SQL creation script.
     */
    public static final void setupDatabase(String sqlCreateScript)
            throws SQLException, FileNotFoundException, IOException,
            ClassNotFoundException {
        if (!databaseSetupDone) {
            synchronized (databaseSetupDone) {
                BufferedReader input = null;
                try {
                    Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
                    java.sql.Connection connection = java.sql.DriverManager
                            .getConnection("jdbc:derby:testdb;create=true");

                    input = new BufferedReader(new FileReader(sqlCreateScript));
                    String line = null;

                    while ((line = input.readLine()) != null) {
                        if (!isSQLComment(line)) {
                            Logger.getLogger("global").log(Level.INFO, line);
                            Statement st = connection.createStatement();
                            try {
                                int i = st.executeUpdate(line); // run the query
                                if (i == -1) {
                                    Logger.getLogger("global").log(
                                            Level.SEVERE, "SQL Error: " + line);
                                }
                            } catch (SQLException e) {
                                String msg = e.getMessage();
                                if (msg.contains("DROP TABLE")) {
                                    continue; // Ignore errors when trying to
                                                // drop tables
                                }
                                Logger.getLogger("global").log(Level.SEVERE,
                                        "SQL Error: ", e);
                            }
                            st.close();
                        }
                    }
                    connection.commit();
                    connection.close();
                    databaseSetupDone = true;
                } finally {
                    try {
                        if (input != null) {
                            // flush and close both "input" and its underlying
                            // FileReader
                            input.close();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    private static boolean isSQLComment(String line) {
        return ((line.startsWith("//")) || (line.startsWith("--")));
    }
}
