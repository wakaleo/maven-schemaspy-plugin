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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.TableColumn;
import net.sourceforge.schemaspy.model.TableIndex;
import net.sourceforge.schemaspy.util.LineWriter;

/**
 * The page that lists all of the columns in the schema,
 * allowing the end user to sort by column's attributes.
 *
 * @author John Currier
 */
public class HtmlColumnsPage extends HtmlFormatter {
    private static HtmlColumnsPage instance = new HtmlColumnsPage();

    /**
     * Singleton: Don't allow instantiation
     */
    private HtmlColumnsPage() {
    }

    /**
     * Singleton accessor
     *
     * @return the singleton instance
     */
    public static HtmlColumnsPage getInstance() {
        return instance;
    }

    /**
     * Returns details about the columns that are displayed on this page.
     *
     * @return
     */
    public List<ColumnInfo> getColumnInfos()
    {
        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();

        columns.add(new ColumnInfo("Table", new ByTableComparator()));
        columns.add(new ColumnInfo("Column", new ByColumnComparator()));
        columns.add(new ColumnInfo("Type", new ByTypeComparator()));
        columns.add(new ColumnInfo("Size", new BySizeComparator()));
        columns.add(new ColumnInfo("Nulls", new ByNullableComparator()));
        columns.add(new ColumnInfo("Auto", new ByAutoUpdateComparator()));
        columns.add(new ColumnInfo("Default", new ByDefaultValueComparator()));

        return columns;
    }

    public class ColumnInfo
    {
        private final String columnName;
        private final Comparator<TableColumn> comparator;

        private ColumnInfo(String columnName, Comparator<TableColumn> comparator)
        {
            this.columnName = columnName;
            this.comparator = comparator;
        }

        public String getColumnName() {
            return columnName;
        }

        public String getLocation() {
            return getLocation(columnName);
        }

        public String getLocation(String colName) {
            return "columns.by" + colName + ".html";
        }

        private Comparator<TableColumn> getComparator() {
            return comparator;
        }

        @Override
        public String toString() {
            return getLocation();
        }
    }

    public void write(Database database, Collection<Table> tables, ColumnInfo columnInfo, boolean showOrphansDiagram, LineWriter html) throws IOException {
        Set<TableColumn> columns = new TreeSet<TableColumn>(columnInfo.getComparator());
        Set<TableColumn> primaryColumns = new HashSet<TableColumn>();
        Set<TableColumn> indexedColumns = new HashSet<TableColumn>();

        for (Table table : tables) {
            columns.addAll(table.getColumns());

            primaryColumns.addAll(table.getPrimaryColumns());
            for (TableIndex index : table.getIndexes()) {
                indexedColumns.addAll(index.getColumns());
            }
        }

        writeHeader(database, columns.size(), showOrphansDiagram, columnInfo, html);

        HtmlTablePage formatter = HtmlTablePage.getInstance();

        for (TableColumn column : columns) {
            formatter.writeColumn(column, column.getTable().getName(), primaryColumns, indexedColumns, true, false, html);
        }

        writeFooter(html);
    }

    private void writeHeader(Database db, int numberOfColumns, boolean hasOrphans, ColumnInfo selectedColumn, LineWriter html) throws IOException {
        writeHeader(db, null, "Columns", hasOrphans, html);

        html.writeln("<table width='100%' border='0'>");
        html.writeln("<tr><td class='container'>");
        writeGeneratedBy(db.getConnectTime(), html);
        html.writeln("</td><td class='container' rowspan='2' align='right' valign='top'>");
        writeLegend(false, false, html);
        html.writeln("</td></tr>");
        html.writeln("<tr valign='top'><td class='container' align='left' valign='top'>");
        html.writeln("<p>");
        html.writeln("<form name='options' action=''>");
        html.writeln(" <label for='showComments'><input type=checkbox id='showComments'>Comments</label>");
        html.writeln(" <label for='showLegend'><input type=checkbox checked id='showLegend'>Legend</label>");
        html.writeln("</form>");
        html.writeln("</table>");

        html.writeln("<div class='indent'>");
        html.write("<b>");
        html.write(db.getName());
        if (db.getSchema() != null) {
            html.write('.');
            html.write(db.getSchema());
        }
        html.write(" contains ");
        html.write(String.valueOf(numberOfColumns));
        html.write(" columns</b> - click on heading to sort:");
        Collection<Table> tables = db.getTables();
        boolean hasTableIds = tables.size() > 0 && tables.iterator().next().getId() != null;
        writeMainTableHeader(hasTableIds, selectedColumn, html);
        html.writeln("<tbody valign='top'>");
    }

    public void writeMainTableHeader(boolean hasTableIds, ColumnInfo selectedColumn, LineWriter out) throws IOException {
        boolean onColumnsPage = selectedColumn != null;
        out.writeln("<a name='columns'></a>");
        out.writeln("<table id='columns' class='dataTable' border='1' rules='groups'>");
        int numCols = 6;    // base number of columns
        if (hasTableIds && !onColumnsPage)
            ++numCols;      // for table id
        if (onColumnsPage)
            ++numCols;      // for table name
        else
            numCols += 2;   // for children and parents
        for (int i = 0; i < numCols; ++i)
            out.writeln("<colgroup>");
        out.writeln("<colgroup class='comment'>");

        out.writeln("<thead align='left'>");
        out.writeln("<tr>");
        if (hasTableIds && !onColumnsPage)
            out.writeln(getTH(selectedColumn, "ID", null, "right"));
        if (onColumnsPage)
            out.writeln(getTH(selectedColumn, "Table", null, null));
        out.writeln(getTH(selectedColumn, "Column", null, null));
        out.writeln(getTH(selectedColumn, "Type", null, null));
        out.writeln(getTH(selectedColumn, "Size", null, null));
        out.writeln(getTH(selectedColumn, "Nulls", "Are nulls allowed?", null));
        out.writeln(getTH(selectedColumn, "Auto", "Is column automatically updated?", null));
        out.writeln(getTH(selectedColumn, "Default", "Default value", null));
        if (!onColumnsPage) {
            out.write("  <th title='Columns in tables that reference this column'>");
            out.writeln("<span class='notSortedByColumn'>Children</span></th>");
            out.write("  <th title='Columns in tables that are referenced by this column'>");
            out.writeln("<span class='notSortedByColumn'>Parents</span></th>");
        }
        out.writeln("  <th title='Comments' class='comment'><span class='notSortedByColumn'>Comments</span></th>");
        out.writeln("</tr>");
        out.writeln("</thead>");
    }

    private String getTH(ColumnInfo selectedColumn, String columnName, String title, String align) {
        StringBuilder buf = new StringBuilder("  <th");

        if (align != null) {
            buf.append(" align='");
            buf.append(align);
            buf.append("'");
        }

        if (title != null) {
            buf.append(" title='");
            buf.append(title);
            buf.append("'");
        }

        if (selectedColumn != null) {
            if (selectedColumn.getColumnName().equals(columnName)) {
                buf.append(" class='sortedByColumn'>");
                buf.append(columnName);
            } else {
                buf.append(" class='notSortedByColumn'>");
                buf.append("<a href='");
                buf.append(selectedColumn.getLocation(columnName));
                buf.append("#columns'><span class='notSortedByColumn'>");
                buf.append(columnName);
                buf.append("</span></a>");
            }
        } else {
            buf.append('>');
            buf.append(columnName);
        }
        buf.append("</th>");

        return buf.toString();
    }

    @Override
    protected void writeFooter(LineWriter html) throws IOException {
        html.writeln("</table>");
        html.writeln("</div>");
        super.writeFooter(html);
    }

    @Override
    protected boolean isColumnsPage() {
        return true;
    }

    private class ByColumnComparator implements Comparator<TableColumn> {
        public int compare(TableColumn column1, TableColumn column2) {
            int rc = column1.getName().compareToIgnoreCase(column2.getName());
            if (rc == 0)
                rc = column1.getTable().compareTo(column2.getTable());
            return rc;
        }
    }

    private class ByTableComparator implements Comparator<TableColumn> {
        public int compare(TableColumn column1, TableColumn column2) {
            int rc = column1.getTable().compareTo(column2.getTable());
            if (rc == 0)
                rc = column1.getName().compareToIgnoreCase(column2.getName());
            return rc;
        }
    }

    private class ByTypeComparator implements Comparator<TableColumn> {
        private final Comparator<TableColumn> bySize = new BySizeComparator();

        public int compare(TableColumn column1, TableColumn column2) {
            int rc = column1.getType().compareToIgnoreCase(column2.getType());
            if (rc == 0) {
                rc = bySize.compare(column1, column2);
            }
            return rc;
        }
    }

    private class BySizeComparator implements Comparator<TableColumn> {
        private final Comparator<TableColumn> byColumn = new ByColumnComparator();

        public int compare(TableColumn column1, TableColumn column2) {
            int rc = column1.getLength() - column2.getLength();
            if (rc == 0) {
                rc = column1.getDecimalDigits() - column2.getDecimalDigits();
                if (rc == 0)
                    rc = byColumn.compare(column1, column2);
            }
            return rc;
        }
    }

    private class ByNullableComparator implements Comparator<TableColumn> {
        private final Comparator<TableColumn> byColumn = new ByColumnComparator();

        public int compare(TableColumn column1, TableColumn column2) {
            int rc = column1.isNullable() == column2.isNullable() ? 0 : column1.isNullable() ? -1 : 1;
            if (rc == 0)
                rc = byColumn.compare(column1, column2);
            return rc;
        }
    }

    private class ByAutoUpdateComparator implements Comparator<TableColumn> {
        private final Comparator<TableColumn> byColumn = new ByColumnComparator();

        public int compare(TableColumn column1, TableColumn column2) {
            int rc = column1.isAutoUpdated() == column2.isAutoUpdated() ? 0 : column1.isAutoUpdated() ? -1 : 1;
            if (rc == 0)
                rc = byColumn.compare(column1, column2);
            return rc;
        }
    }

    private class ByDefaultValueComparator implements Comparator<TableColumn> {
        private final Comparator<TableColumn> byColumn = new ByColumnComparator();

        public int compare(TableColumn column1, TableColumn column2) {
            int rc = String.valueOf(column1.getDefaultValue()).compareToIgnoreCase(String.valueOf(column2.getDefaultValue()));
            if (rc == 0)
                rc = byColumn.compare(column1, column2);
            return rc;
        }
    }
}