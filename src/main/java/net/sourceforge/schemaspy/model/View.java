/*
 * This file is a part of the SchemaSpy project (http://schemaspy.sourceforge.net).
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 John Currier
 *
 * SchemaSpy is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * SchemaSpy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.sourceforge.schemaspy.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Treat views as tables that have no rows and are represented by the SQL that
 * defined them.
 */
public class View extends Table {
    private String viewSql;

    /**
     * @param db
     * @param schema
     * @param name
     * @param remarks
     * @param viewSql
     * @param properties
     * @param excludeIndirectColumns
     * @param excludeColumns
     * @throws SQLException
     */
    public View(Database db, String schema, String name, String remarks, String viewSql,
                Properties properties,
                Pattern excludeIndirectColumns, Pattern excludeColumns) throws SQLException {
        super(db, schema, name, remarks, properties, excludeIndirectColumns, excludeColumns);

        if (viewSql == null)
            viewSql = fetchViewSql();

        if (viewSql != null && viewSql.trim().length() > 0)
            this.viewSql = viewSql;
    }

    /**
     * @return
     */
    @Override
    public boolean isView() {
        return true;
    }

    @Override
    public String getViewSql() {
        return viewSql;
    }

    @Override
    protected int fetchNumRows() {
        return 0;
    }

    /**
     * Extract the SQL that describes this view from the database
     *
     * @return
     * @throws SQLException
     */
    private String fetchViewSql() throws SQLException {
        String selectViewSql = properties.getProperty("selectViewSql");
        if (selectViewSql == null)
            return null;

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = db.prepareStatement(selectViewSql, getName());
            rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    return rs.getString("view_definition");
                } catch (SQLException tryOldName) {
                    return rs.getString("text");
                }
            }
            return null;
        } catch (SQLException sqlException) {
            System.err.println(selectViewSql);
            throw sqlException;
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
        }
    }
}
