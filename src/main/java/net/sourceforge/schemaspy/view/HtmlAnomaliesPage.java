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
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import net.sourceforge.schemaspy.DbAnalyzer;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.ForeignKeyConstraint;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.TableColumn;
import net.sourceforge.schemaspy.util.LineWriter;

/**
 * This page lists all of the 'things that might not be quite right'
 * about the schema.
 *
 * @author John Currier
 */
public class HtmlAnomaliesPage extends HtmlFormatter {
    private static HtmlAnomaliesPage instance = new HtmlAnomaliesPage();

    /**
     * Singleton: Don't allow instantiation
     */
    private HtmlAnomaliesPage() {
    }

    /**
     * Singleton accessor
     *
     * @return the singleton instance
     */
    public static HtmlAnomaliesPage getInstance() {
        return instance;
    }

    public void write(Database database, Collection<Table> tables, List<? extends ForeignKeyConstraint> impliedConstraints, boolean hasOrphans, LineWriter out) throws IOException {
        writeHeader(database, hasOrphans, out);
        writeImpliedConstraints(impliedConstraints, out);
        writeTablesWithoutIndexes(DbAnalyzer.getTablesWithoutIndexes(new HashSet<Table>(tables)), out);
        writeUniqueNullables(DbAnalyzer.getMustBeUniqueNullableColumns(new HashSet<Table>(tables)), out);
        writeTablesWithOneColumn(DbAnalyzer.getTablesWithOneColumn(tables), out);
        writeTablesWithIncrementingColumnNames(DbAnalyzer.getTablesWithIncrementingColumnNames(tables), out);
        writeDefaultNullStrings(DbAnalyzer.getDefaultNullStringColumns(new HashSet<Table>(tables)), out);
        writeFooter(out);
    }

    private void writeHeader(Database database, boolean hasOrphans, LineWriter html) throws IOException {
        writeHeader(database, null, "Anomalies", hasOrphans, html);
        html.writeln("<table width='100%'>");
        if (sourceForgeLogoEnabled())
            html.writeln("  <tr><td class='container' align='right' valign='top' colspan='2'><a href='http://sourceforge.net' target='_blank'><img src='http://sourceforge.net/sflogo.php?group_id=137197&amp;type=1' alt='SourceForge.net' border='0' height='31' width='88'></a></td></tr>");
        html.writeln("<tr>");
        html.writeln("<td class='container'><b>Things that might not be 'quite right' about your schema:</b></td>");
        html.writeln("<td class='container' align='right'>");
        writeFeedMe(html);
        html.writeln("</td></tr></table>");
        html.writeln("<ul>");
    }

    private void writeImpliedConstraints(List<? extends ForeignKeyConstraint> impliedConstraints, LineWriter out) throws IOException {
        out.writeln("<li>");
        out.writeln("<b>Columns whose name and type imply a relationship to another table's primary key:</b>");
        int numDetected = 0;

        for (ForeignKeyConstraint impliedConstraint : impliedConstraints) {
            Table childTable = impliedConstraint.getChildTable();
            if (!childTable.isView()) {
                ++numDetected;
            }
        }

        if (numDetected > 0) {
            out.writeln("<table class='dataTable' border='1' rules='groups'>");
            out.writeln("<colgroup>");
            out.writeln("<colgroup>");
            out.writeln("<thead align='left'>");
            out.writeln("<tr>");
            out.writeln("  <th>Child Column</th>");
            out.writeln("  <th>Implied Parent Column</th>");
            out.writeln("</tr>");
            out.writeln("</thead>");
            out.writeln("<tbody>");

            for (ForeignKeyConstraint impliedConstraint : impliedConstraints) {
                Table childTable = impliedConstraint.getChildTable();
                if (!childTable.isView()) {
                    out.writeln(" <tr>");

                    out.write("  <td class='detail'>");
                    String tableName = childTable.getName();
                    out.write("<a href='tables/");
                    out.write(tableName);
                    out.write(".html'>");
                    out.write(tableName);
                    out.write("</a>.");
                    out.write(ForeignKeyConstraint.toString(impliedConstraint.getChildColumns()));
                    out.writeln("</td>");

                    out.write("  <td class='detail'>");
                    tableName = impliedConstraint.getParentTable().getName();
                    out.write("<a href='tables/");
                    out.write(tableName);
                    out.write(".html'>");
                    out.write(tableName);
                    out.write("</a>.");
                    out.write(ForeignKeyConstraint.toString(impliedConstraint.getParentColumns()));
                    out.writeln("</td>");

                    out.writeln(" </tr>");
                }
            }

            out.writeln("</tbody>");
            out.writeln("</table>");
        }
        writeSummary(numDetected, out);
        out.writeln("<p></li>");
    }

    private void writeUniqueNullables(List<TableColumn> uniqueNullables, LineWriter out) throws IOException {
        out.writeln("<li>");
        out.writeln("<b>Columns that are flagged as both 'nullable' and 'must be unique':</b>");
        writeColumnBasedAnomaly(uniqueNullables, out);
        out.writeln("<p></li>");
    }

    private void writeTablesWithoutIndexes(List<Table> unindexedTables, LineWriter out) throws IOException {
        out.writeln("<li>");
        out.writeln("<b>Tables without indexes:</b>");
        if (!unindexedTables.isEmpty()) {
            out.writeln("<table class='dataTable' border='1' rules='groups'>");
            out.writeln("<colgroup>");
            if (displayNumRows)
                out.writeln("<colgroup>");
            out.writeln("<thead align='left'>");
            out.writeln("<tr>");
            out.write("  <th>Table</th>");
            if (displayNumRows)
                out.write("<th>Rows</th>");
            out.writeln();
            out.writeln("</tr>");
            out.writeln("</thead>");
            out.writeln("<tbody>");

            for (Table table : unindexedTables) {
                out.writeln(" <tr>");
                out.write("  <td class='detail'>");
                out.write("<a href='tables/");
                out.write(table.getName());
                out.write(".html'>");
                out.write(table.getName());
                out.write("</a>");
                out.writeln("</td>");
                if (displayNumRows) {
                    out.write("  <td class='detail' align='right'>");
                    if (!table.isView())
                        out.write(String.valueOf(NumberFormat.getIntegerInstance().format(table.getNumRows())));
                    out.writeln("</td>");
                }
                out.writeln(" </tr>");
            }

            out.writeln("</tbody>");
            out.writeln("</table>");
        }
        writeSummary(unindexedTables.size(), out);
        out.writeln("<p></li>");
    }

    private void writeTablesWithIncrementingColumnNames(List<Table> tables, LineWriter out) throws IOException {
        out.writeln("<li>");
        out.writeln("<b>Tables with incrementing column names, potentially indicating denormalization:</b>");
        if (!tables.isEmpty()) {
            out.writeln("<table class='dataTable' border='1' rules='groups'>");
            out.writeln("<thead align='left'>");
            out.writeln("<tr>");
            out.writeln("  <th>Table</th>");
            out.writeln("</tr>");
            out.writeln("</thead>");
            out.writeln("<tbody>");

            for (Table table : tables) {
                out.writeln(" <tr>");
                out.write("  <td class='detail'>");
                out.write("<a href='tables/");
                out.write(table.getName());
                out.write(".html'>");
                out.write(table.getName());
                out.write("</a>");
                out.writeln("</td>");
                out.writeln(" </tr>");
            }

            out.writeln("</tbody>");
            out.writeln("</table>");
        }
        writeSummary(tables.size(), out);
        out.writeln("<p></li>");
    }

    private void writeTablesWithOneColumn(List<Table> tables, LineWriter out) throws IOException {
        out.writeln("<li>");
        out.write("<b>Tables that contain a single column:</b>");
        if (!tables.isEmpty()) {
            out.writeln("<table class='dataTable' border='1' rules='groups'>");
            out.writeln("<colgroup>");
            out.writeln("<colgroup>");
            out.writeln("<thead align='left'>");
            out.writeln("<tr>");
            out.writeln("  <th>Table</th>");
            out.writeln("  <th>Column</th>");
            out.writeln("</tr>");
            out.writeln("</thead>");
            out.writeln("<tbody>");

            for (Table table : tables) {
                out.writeln(" <tr>");
                out.write("  <td class='detail'>");
                out.write("<a href='tables/");
                out.write(table.getName());
                out.write(".html'>");
                out.write(table.getName());
                out.write("</a></td><td class='detail'>");
                out.write(table.getColumns().get(0).toString());
                out.writeln("</td>");
                out.writeln(" </tr>");
            }

            out.writeln("</tbody>");
            out.writeln("</table>");
        }
        writeSummary(tables.size(), out);
        out.writeln("<p></li>");
    }

    private void writeDefaultNullStrings(List<TableColumn> uniqueNullables, LineWriter out) throws IOException {
        out.writeln("<li>");
        out.writeln("<b>Columns whose default value is the word 'NULL' or 'null', but the SQL NULL value may have been intended:</b>");
        writeColumnBasedAnomaly(uniqueNullables, out);
        out.writeln("<p></li>");
    }

    private void writeColumnBasedAnomaly(List<TableColumn> columns, LineWriter out) throws IOException {
        if (!columns.isEmpty()) {
            out.writeln("<table class='dataTable' border='1' rules='groups'>");
            out.writeln("<thead align='left'>");
            out.writeln("<tr>");
            out.writeln("  <th>Column</th>");
            out.writeln("</tr>");
            out.writeln("</thead>");
            out.writeln("<tbody>");
            for (TableColumn column : columns) {
                out.writeln(" <tr>");
                out.write("  <td class='detail'>");
                String tableName = column.getTable().getName();
                out.write("<a href='tables/");
                out.write(tableName);
                out.write(".html'>");
                out.write(tableName);
                out.write("</a>.");
                out.write(column.getName());
                out.writeln("</td>");
                out.writeln(" </tr>");
            }

            out.writeln("</tbody>");
            out.writeln("</table>");
        }
        writeSummary(columns.size(), out);
    }

    private void writeSummary(int numAnomalies, LineWriter out) throws IOException {
        switch (numAnomalies) {
            case 0:
                out.write("<br>Anomaly not detected");
                break;
            case 1:
                out.write("1 instance of anomaly detected");
                break;
            default:
                out.write(numAnomalies + " instances of anomaly detected");
        }
    }

    @Override
    protected void writeFooter(LineWriter out) throws IOException {
        out.writeln("</ul>");
        super.writeFooter(out);
    }

    @Override
    protected boolean isAnomaliesPage() {
        return true;
    }
}
