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
package net.sourceforge.schemaspy.view;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.TableColumn;
import net.sourceforge.schemaspy.model.TableIndex;

public class DotNode {
    private final Table table;
    private final DotNodeConfig config;
    private final String path;
    private final Set<TableColumn> excludedColumns = new HashSet<TableColumn>();
    private final String lineSeparator = System.getProperty("line.separator");
    private final boolean displayNumRows = Config.getInstance().isNumRowsEnabled();

    /**
     * Create a DotNode that is a focal point of a diagram.
     * That is, all of its columns are displayed in addition to the details
     * of those columns.
     *
     * @param table Table
     * @param path String
     */
    public DotNode(Table table, String path) {
        this(table, path, new DotNodeConfig(true, true));
    }

    public DotNode(Table table, String path, DotNodeConfig config) {
        this.table = table;
        this.path = path + (table.isRemote() ? ("../../" + table.getSchema() + "/tables/") : "");
        this.config = config;
    }

    /**
     * Create a DotNode and specify whether it displays its columns.
     * The details of the optional columns (e.g. type, size) are not displayed.
     *
     * @param table Table
     * @param showColumns boolean
     * @param path String
     */
    public DotNode(Table table, boolean showColumns, String path) {
        this(table, path, showColumns ? new DotNodeConfig(true, false) : new DotNodeConfig());
    }

    public void setShowImplied(boolean showImplied) {
        config.showImpliedRelationships = showImplied;
    }

    public Table getTable() {
        return table;
    }

    public void excludeColumn(TableColumn column) {
        excludedColumns.add(column);
    }

    @Override
    public String toString() {
        StyleSheet css = StyleSheet.getInstance();
        StringBuilder buf = new StringBuilder();
        String tableName = table.getName();
        // fully qualified table name (optionally prefixed with schema)
        String fqTableName = (table.isRemote() ? table.getSchema() + "." : "") + tableName;
        String colspan = config.showColumnDetails ? "COLSPAN=\"2\" " : "COLSPAN=\"3\" ";

        buf.append("  \"" + fqTableName + "\" [" + lineSeparator);
        buf.append("    label=<" + lineSeparator);
        buf.append("    <TABLE BORDER=\"" + (config.showColumnDetails ? "2" : "0") + "\" CELLBORDER=\"1\" CELLSPACING=\"0\" BGCOLOR=\"" + css.getTableBackground() + "\">" + lineSeparator);
        buf.append("      <TR>");
        buf.append("<TD COLSPAN=\"3\" BGCOLOR=\"" + css.getTableHeadBackground() + "\" ALIGN=\"CENTER\">" + fqTableName + "</TD>");
        buf.append("</TR>" + lineSeparator);

        boolean skippedTrivial = false;

        if (config.showColumns) {
            List<TableColumn> primaryColumns = table.getPrimaryColumns();
            Set<TableColumn> indexColumns = new HashSet<TableColumn>();

            for (TableIndex index : table.getIndexes()) {
                indexColumns.addAll(index.getColumns());
            }
            indexColumns.removeAll(primaryColumns);

            for (TableColumn column : table.getColumns()) {
                if (config.showTrivialColumns || config.showColumnDetails || column.isPrimary() || column.isForeignKey() || indexColumns.contains(column)) {
                    buf.append("      <TR>");
                    buf.append("<TD PORT=\"" + column.getName() + "\" " + colspan);
                    if (excludedColumns.contains(column))
                        buf.append("BGCOLOR=\"" + css.getExcludedColumnBackgroundColor() + "\" ");
                    else if (primaryColumns.contains(column))
                        buf.append("BGCOLOR=\"" + css.getPrimaryKeyBackground() + "\" ");
                    else if (indexColumns.contains(column))
                        buf.append("BGCOLOR=\"" + css.getIndexedColumnBackground() + "\" ");
                    buf.append("ALIGN=\"LEFT\">");
                    buf.append(column.getName());
                    buf.append("</TD>");
                    if (config.showColumnDetails) {
                        buf.append("<TD PORT=\"");
                        buf.append(column.getName());
                        buf.append(".type\" ALIGN=\"LEFT\">");
                        buf.append(column.getType().toLowerCase());
                        buf.append("[");
                        buf.append(column.getDetailedSize());
                        buf.append("]</TD>");
                    }
                    buf.append("</TR>" + lineSeparator);
                } else {
                    skippedTrivial = true;
                }
            }
        }

        if (skippedTrivial || !config.showColumns) {
            buf.append("      <TR><TD PORT=\"elipses\" COLSPAN=\"3\" ALIGN=\"LEFT\">...</TD></TR>" + lineSeparator);
        }

        buf.append("      <TR>");
        buf.append("<TD ALIGN=\"LEFT\" BGCOLOR=\"" + css.getBodyBackground() + "\">");
        int numParents = config.showImpliedRelationships ? table.getNumParents() : table.getNumNonImpliedParents();
        if (numParents > 0 || config.showColumnDetails)
            buf.append("&lt; " + numParents);
        else
            buf.append("  ");
        buf.append("</TD>");
        buf.append("<TD ALIGN=\"RIGHT\" BGCOLOR=\"" + css.getBodyBackground() + "\">");
        if (table.isView())
            buf.append("view");
        else {
            final int numRows = table.getNumRows();
            if (displayNumRows && numRows != -1) {
                buf.append(NumberFormat.getInstance().format(numRows));
                buf.append(" row");
                if (numRows != 1)
                    buf.append('s');
            } else {
                buf.append("  ");
            }
        }
        buf.append("</TD>");
        buf.append("<TD ALIGN=\"RIGHT\" BGCOLOR=\"" + css.getBodyBackground() + "\">");
        int numChildren = config.showImpliedRelationships ? table.getNumChildren() : table.getNumNonImpliedChildren();
        if (numChildren > 0 || config.showColumnDetails)
            buf.append(numChildren + " &gt;");
        else
            buf.append("  ");
        buf.append("</TD></TR>" + lineSeparator);

        buf.append("    </TABLE>>" + lineSeparator);
        if (!table.isRemote() || Config.getInstance().isOneOfMultipleSchemas())
            buf.append("    URL=\"" + path + toNCR(tableName) + ".html\"" + lineSeparator);
        buf.append("    tooltip=\"" + toNCR(fqTableName) + "\"" + lineSeparator);
        buf.append("  ];");

        return buf.toString();
    }

    /**
     * Translates specified string to Numeric Character Reference (NCR).
     * This (hopefully) allows Unicode languages to be displayed correctly.<p>
     * The basis for this code was found
     * <a href='http://d.hatena.ne.jp/etherealmaro/20060806#1154886500'>here</a>.
     *
     * @param str
     * @return
     */
    private static String toNCR(String str) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); ++i) {
            char ch = str.charAt(i);
            if (ch <= 127) {    // don't confuse things unless necessary
                result.append(ch);
            } else {
                result.append("&#");
                result.append(Integer.parseInt(Integer.toHexString(ch), 16));
                result.append(";");
            }
        }
        return result.toString();
    }

    public static class DotNodeConfig {
        private final boolean showColumns;
        private boolean showTrivialColumns;
        private final boolean showColumnDetails;
        private boolean showImpliedRelationships;

        /**
         * Nothing but table name and counts are displayed
         */
        public DotNodeConfig() {
            showColumns = showTrivialColumns = showColumnDetails = showImpliedRelationships = false;
        }

        public DotNodeConfig(boolean showTrivialColumns, boolean showColumnDetails) {
            showColumns = true;
            this.showTrivialColumns = showTrivialColumns;
            this.showColumnDetails = showColumnDetails;
        }
    }
}