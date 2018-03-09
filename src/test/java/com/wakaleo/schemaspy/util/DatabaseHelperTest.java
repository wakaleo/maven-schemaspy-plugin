/*
 * TestDatabaseHelperTest.java
 * JUnit based test
 *
 * Created on 16 May 2007, 10:41
 */

package com.wakaleo.schemaspy.util;

import org.junit.Test;

import java.sql.ResultSet;

import static org.junit.Assert.assertNotNull;

/**
 * 
 * @author john
 */
public class DatabaseHelperTest {

    // public static final String TESTDB_URL =


    @Test
    public void testSetupDatabase() throws Exception {
        DatabaseHelper.setupDatabase("src/test/resources/sql/testdb.sql");

        java.sql.Connection connection = java.sql.DriverManager
                .getConnection("jdbc:derby:testdb");

        ResultSet rs = connection.createStatement().executeQuery(
                "select * from employee");
        assertNotNull(rs);

        rs = connection.createStatement().executeQuery("select * from item");
        assertNotNull(rs);

        rs = connection.createStatement()
                .executeQuery("select * from customer");
        assertNotNull(rs);

        rs = connection.createStatement().executeQuery(
                "select * from salesorder");
        assertNotNull(rs);
        
        connection.close();

    }

}
