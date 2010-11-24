package net.sourceforge.schemaspy.view;

import java.io.*;
import java.text.*;
import java.util.*;
import net.sourceforge.schemaspy.*;
import net.sourceforge.schemaspy.model.*;
import net.sourceforge.schemaspy.util.*;

public class HtmlAnomaliesPage extends HtmlFormatter {
    private static HtmlAnomaliesPage instance = new HtmlAnomaliesPage();

    /**
     * Singleton - don't allow creation
     */
    private HtmlAnomaliesPage() {
    }

    public static HtmlAnomaliesPage getInstance() {
        return instance;
    }

    public void write(Database database, Collection tables, List impliedConstraints, boolean hasOrphans, LineWriter out) throws IOException {
        writeHeader(database, hasOrphans, out);
        writeImpliedConstraints(impliedConstraints, out);
        writeTablesWithoutIndexes(DBAnalyzer.getTablesWithoutIndexes(new HashSet(tables)), out);
        writeUniqueNullables(DBAnalyzer.getMustBeUniqueNullableColumns(new HashSet(tables)), out);
        writeTablesWithOneColumn(DBAnalyzer.getTablesWithOneColumn(tables), out);
        writeTablesWithIncrementingColumnNames(DBAnalyzer.getTablesWithIncrementingColumnNames(tables), out);
        writeDefaultNullStrings(DBAnalyzer.getDefaultNullStringColumns(new HashSet(tables)), out);
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

    private void writeImpliedConstraints(List impliedConstraints, LineWriter out) throws IOException {
        out.writeln("<li>");
        out.writeln("<b>Columns whose name and type imply a relationship to another table's primary key:</b>");
        int numDetected = 0;
        Iterator iter = impliedConstraints.iterator();
        while (iter.hasNext()) {
            ForeignKeyConstraint impliedConstraint = (ForeignKeyConstraint)iter.next();
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
            iter = impliedConstraints.iterator();
            while (iter.hasNext()) {
                ForeignKeyConstraint impliedConstraint = (ForeignKeyConstraint)iter.next();
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
        out.writeln("<p/></li>");
    }

    private void writeUniqueNullables(List uniqueNullables, LineWriter out) throws IOException {
        out.writeln("<li>");
        out.writeln("<b>Columns that are flagged as both 'nullable' and 'must be unique':</b>");
        writeColumnBasedAnomaly(uniqueNullables, out);
        out.writeln("<p/></li>");
    }

    private void writeTablesWithoutIndexes(List unindexedTables, LineWriter out) throws IOException {
        out.writeln("<li>");
        out.writeln("<b>Tables without indexes:</b>");
        if (!unindexedTables.isEmpty()) {
            out.writeln("<table class='dataTable' border='1' rules='groups'>");
            out.writeln("<colgroup>");
            out.writeln("<colgroup>");
            out.writeln("<thead align='left'>");
            out.writeln("<tr>");
            out.writeln("  <th>Table</th><th>Rows</th>");
            out.writeln("</tr>");
            out.writeln("</thead>");
            out.writeln("<tbody>");
            Iterator iter = unindexedTables.iterator();
            while (iter.hasNext()) {
                Table table = (Table)iter.next();
                out.writeln(" <tr>");
                out.write("  <td class='detail'>");
                out.write("<a href='tables/");
                out.write(table.getName());
                out.write(".html'>");
                out.write(table.getName());
                out.write("</a>");
                out.writeln("</td>");
                out.write("  <td class='detail' align='right'>");
                if (!table.isView())
                    out.write(String.valueOf(NumberFormat.getIntegerInstance().format(table.getNumRows())));
                out.writeln("</td>");
                out.writeln(" </tr>");
            }

            out.writeln("</tbody>");
            out.writeln("</table>");
        }
        writeSummary(unindexedTables.size(), out);
        out.writeln("<p/></li>");
    }

    private void writeTablesWithIncrementingColumnNames(List tables, LineWriter out) throws IOException {
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
            Iterator iter = tables.iterator();
            while (iter.hasNext()) {
                Table table = (Table)iter.next();
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
        out.writeln("<p/></li>");
    }

    private void writeTablesWithOneColumn(List tables, LineWriter out) throws IOException {
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
            Iterator iter = tables.iterator();
            while (iter.hasNext()) {
                Table table = (Table)iter.next();
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
        out.writeln("<p/></li>");
    }

    private void writeDefaultNullStrings(List uniqueNullables, LineWriter out) throws IOException {
        out.writeln("<li>");
        out.writeln("<b>Columns whose default value is the word 'NULL' or 'null', but the SQL NULL value may have been intended:</b>");
        writeColumnBasedAnomaly(uniqueNullables, out);
        out.writeln("<p/></li>");
    }

    private void writeColumnBasedAnomaly(List columns, LineWriter out) throws IOException {
        if (!columns.isEmpty()) {
            out.writeln("<table class='dataTable' border='1' rules='groups'>");
            out.writeln("<thead align='left'>");
            out.writeln("<tr>");
            out.writeln("  <th>Column</th>");
            out.writeln("</tr>");
            out.writeln("</thead>");
            out.writeln("<tbody>");
            Iterator iter = columns.iterator();
            while (iter.hasNext()) {
                TableColumn column = (TableColumn)iter.next();
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

    protected void writeFooter(LineWriter out) throws IOException {
        out.writeln("</ul>");
        super.writeFooter(out);
    }

    protected boolean isAnomaliesPage() {
        return true;
    }
}
