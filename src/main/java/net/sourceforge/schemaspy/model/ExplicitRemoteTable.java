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

import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A remote table (exists in another schema) that was explicitly created via XML metadata.
 *
 * @author John Currier
 */
public class ExplicitRemoteTable extends RemoteTable {
    private static final Pattern excludeNone = Pattern.compile("[^.]");

    public ExplicitRemoteTable(Database db, String schema, String name, String baseSchema) throws SQLException {
        super(db, schema, name, baseSchema, null, excludeNone, excludeNone);
    }

    @Override
    public void connectForeignKeys(Map<String, Table> tables, Pattern excludeIndirectColumns, Pattern excludeColumns) throws SQLException {
        // this probably won't work, so ignore any failures...but try anyways just in case
        try {
            super.connectForeignKeys(tables, excludeIndirectColumns, excludeColumns);
        } catch (SQLException ignore) {}
    }
}