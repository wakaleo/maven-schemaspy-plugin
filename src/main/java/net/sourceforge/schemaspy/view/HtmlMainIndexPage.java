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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.util.HtmlEncoder;
import net.sourceforge.schemaspy.util.LineWriter;

/**
 * The main index that contains all tables and views that were evaluated
 *
 * @author John Currier
 */
public class HtmlMainIndexPage extends HtmlFormatter {
    private static HtmlMainIndexPage instance = new HtmlMainIndexPage();
    private final NumberFormat integerFormatter = NumberFormat.getIntegerInstance();

    /**
     * Singleton: Don't allow instantiation
     */
    private HtmlMainIndexPage() {
    }

    /**
     * Singleton accessor
     *
     * @return the singleton instance
     */
    public static HtmlMainIndexPage getInstance() {
        return instance;
    }

    public void write(Database database, Collection<Table> tables, boolean showOrphansDiagram, LineWriter html) throws IOException {
        Set<Table> byName = new TreeSet<Table>(new Comparator<Table>() {
            public int compare(Table table1, Table table2) {
                return table1.compareTo(table2);
            }
        });
        byName.addAll(tables);

        boolean showIds = false;
        int numViews = 0;
        boolean comments = false;

        for (Table table : byName) {
            if (table.isView())
                ++numViews;
            showIds |= table.getId() != null;
            if (table.getComments() != null)
                comments = true;
        }

        writeHeader(database, byName.size() - numViews, numViews, showIds, showOrphansDiagram, comments, html);

        int numTableCols = 0;
        int numViewCols = 0;
        int numRows = 0;
        for (Table table : byName) {
            writeLineItem(table, showIds, html);

            if (!table.isView())
                numTableCols += table.getColumns().size();
            else
                numViewCols += table.getColumns().size();
            numRows += table.getNumRows();
        }

        writeFooter(byName.size() - numViews, numTableCols, numViews, numViewCols, numRows, html);
    }

    private void writeHeader(Database db, int numberOfTables, int numberOfViews, boolean showIds, boolean hasOrphans, boolean hasComments, LineWriter html) throws IOException {
        List<String> javascript = new ArrayList<String>();

        // we can't use the hard-coded even odd technique that we use
        // everywhere else because we're dynamically changing the visibility
        // of tables/views within the list
        javascript.add("$(function(){");
        javascript.add("  associate($('#showTables'), $('.tbl'));");
        javascript.add("  associate($('#showViews'),  $('.view'));");
        javascript.add("  jQuery.fn.alternateRowColors = function() {");
        javascript.add("    $('tbody tr:visible').each(function(i) {");
        javascript.add("      if (i % 2 == 0) {");
        javascript.add("        $(this).removeClass('even').addClass('odd');");
        javascript.add("      } else {");
        javascript.add("        $(this).removeClass('odd').addClass('even');");
        javascript.add("      }");
        javascript.add("    });");
        javascript.add("    return this;");
        javascript.add("  };");
        javascript.add("  $('#showTables, #showViews').click(function() {");
        javascript.add("    $('table.dataTable').alternateRowColors();");
        javascript.add("  });");
        javascript.add("  $('table.dataTable').alternateRowColors();");
        javascript.add("})");

        writeHeader(db, null, null, hasOrphans, javascript, html);
        html.writeln("<table width='100%'>");
        html.writeln(" <tr><td class='container'>");
        writeGeneratedBy(db.getConnectTime(), html);
        html.writeln(" </td></tr>");
        html.writeln(" <tr>");
        html.write("  <td class='container'>Database Type: ");
        html.write(db.getDatabaseProduct());
        html.writeln("  </td>");
        html.writeln("  <td class='container' align='right' valign='top' rowspan='3'>");
        if (sourceForgeLogoEnabled())
            html.writeln("    <a href='http://sourceforge.net' target='_blank'><img src='http://sourceforge.net/sflogo.php?group_id=137197&amp;type=1' alt='SourceForge.net' border='0' height='31' width='88'></a><br>");
        html.write("    <br>");
        writeFeedMe(html);
        html.writeln("  </td>");
        html.writeln(" </tr>");
        html.writeln(" <tr>");
        html.write("  <td class='container'>");
        String xmlName = db.getName();
        if (db.getSchema() != null)
            xmlName += '.' + db.getSchema();
        html.write("<br><a href='" + xmlName + ".xml' title='XML Representation'>XML Representation</a>");
        html.write("<br><a href='insertionOrder.txt' title='Useful for loading data into a database'>Insertion Order</a>&nbsp;");
        html.write("<a href='deletionOrder.txt' title='Useful for purging data from a database'>Deletion Order</a>");
        html.write("&nbsp;(for database loading/purging scripts)");
        html.writeln("</td>");
        html.writeln(" </tr>");
        html.writeln("</table>");

        html.writeln("<div class='indent'>");
        html.write("<p>");
        html.write("<b>");
        if (numberOfViews == 0) {
            html.writeln("<label for='showTables' style='display:none;'><input type='checkbox' id='showTables' checked></label>");
        } else if (numberOfTables == 0) {
            html.writeln("<label for='showViews' style='display:none;'><input type='checkbox' id='showViews' checked></label>");
        } else {
            html.write("<label for='showTables'><input type='checkbox' id='showTables' checked>Tables</label>");
            html.write(" <label for='showViews'><input type='checkbox' id='showViews' checked>Views</label>");
        }

        html.writeln(" <label for='showComments'><input type=checkbox " + (hasComments  ? "checked " : "") + "id='showComments'>Comments</label>");
        html.writeln("</b>");

        html.writeln("<table class='dataTable' border='1' rules='groups'>");
        int numGroups = 4 + (showIds ? 1 : 0) + (displayNumRows ? 1 : 0);
        for (int i = 0; i < numGroups; ++i)
            html.writeln("<colgroup>");
        html.writeln("<colgroup class='comment'>");
        html.writeln("<thead align='left'>");
        html.writeln("<tr>");
        String tableHeading;
        if (numberOfViews == 0)
            tableHeading = "Table";
        else if (numberOfTables == 0)
            tableHeading = "View";
        else
            tableHeading = "Table / View";
        html.writeln("  <th valign='bottom'>" + tableHeading + "</th>");
        if (showIds)
            html.writeln("  <th align='center' valign='bottom'>ID</th>");
        html.writeln("  <th align='right' valign='bottom'>Children</th>");
        html.writeln("  <th align='right' valign='bottom'>Parents</th>");
        html.writeln("  <th align='right' valign='bottom'>Columns</th>");
        if (displayNumRows)
            html.writeln("  <th align='right' valign='bottom'>Rows</th>");
        html.writeln("  <th class='comment' align='left' valign='bottom'>Comments</th>");
        html.writeln("</tr>");
        html.writeln("</thead>");
        html.writeln("<tbody>");
    }

    private void writeLineItem(Table table, boolean showIds, LineWriter html) throws IOException {
        html.write(" <tr class='" + (table.isView() ? "view" : "tbl") + "' valign='top'>");
        html.write("  <td class='detail'><a href='tables/");
        html.write(table.getName());
        html.write(".html'>");
        html.write(table.getName());
        html.writeln("</a></td>");

        if (showIds) {
            html.write("  <td class='detail' align='right'>");
            Object id = table.getId();
            if (id != null)
                html.write(String.valueOf(id));
            else
                html.writeln("&nbsp;");
            html.writeln("</td>");
        }

        html.write("  <td class='detail' align='right'>");
        int numRelatives = table.getNumNonImpliedChildren();
        if (numRelatives != 0)
            html.write(String.valueOf(integerFormatter.format(numRelatives)));
        html.writeln("</td>");
        html.write("  <td class='detail' align='right'>");
        numRelatives = table.getNumNonImpliedParents();
        if (numRelatives != 0)
            html.write(String.valueOf(integerFormatter.format(numRelatives)));
        html.writeln("</td>");

        html.write("  <td class='detail' align='right'>");
        html.write(String.valueOf(integerFormatter.format(table.getColumns().size())));
        html.writeln("</td>");

        if (displayNumRows) {
            html.write("  <td class='detail' align='right'>");
            if (!table.isView())
                html.write(String.valueOf(integerFormatter.format(table.getNumRows())));
            else
                html.write("<span title='Views contain no real rows'>view</span>");
            html.writeln("</td>");
        }
        html.write("  <td class='comment detail'>");
        String comments = table.getComments();
        if (comments != null) {
            if (encodeComments)
                for (int i = 0; i < comments.length(); ++i)
                    html.write(HtmlEncoder.encodeToken(comments.charAt(i)));
            else
                html.write(comments);
        }
        html.writeln("</td>");
        html.writeln("  </tr>");
    }

    protected void writeFooter(int numTables, int numTableCols, int numViews, int numViewCols, int numRows, LineWriter html) throws IOException {
        html.writeln("  <tr>");
        html.writeln("    <td class='detail'>&nbsp;</td>");
        html.writeln("    <td class='detail'>&nbsp;</td>");
        html.writeln("    <td class='detail'>&nbsp;</td>");
        html.writeln("    <td class='detail'>&nbsp;</td>");
        if (displayNumRows)
            html.writeln("    <td class='detail'>&nbsp;</td>");
        html.writeln("    <td class='comment detail'>&nbsp;</td>");
        html.writeln("  </tr>");
        String name = numTables == 1 ? " Table" : " Tables";
        html.writeln("  <tr class='tbl'>");
        html.writeln("    <td class='detail'><b>" + integerFormatter.format(numTables) + name + "</b></td>");
        html.writeln("    <td class='detail'>&nbsp;</td>");
        html.writeln("    <td class='detail'>&nbsp;</td>");
        html.writeln("    <td class='detail' align='right'><b>" + integerFormatter.format(numTableCols) + "</b></td>");
        if (displayNumRows)
            html.writeln("    <td class='detail' align='right'><b>" + integerFormatter.format(numRows) + "</b></td>");
        html.writeln("    <td class='comment detail'>&nbsp;</td>");
        html.writeln("  </tr>");
        name = numViews == 1 ? " View" : " Views";
        html.writeln("  <tr class='view'>");
        html.writeln("    <td class='detail'><b>" + integerFormatter.format(numViews) + name + "</b></td>");
        html.writeln("    <td class='detail'>&nbsp;</td>");
        html.writeln("    <td class='detail'>&nbsp;</td>");
        html.writeln("    <td class='detail' align='right'><b>" + integerFormatter.format(numViewCols) + "</b></td>");
        if (displayNumRows)
            html.writeln("    <td class='detail'>&nbsp;</td>");
        html.writeln("    <td class='comment detail'>&nbsp;</td>");
        html.writeln("  </tr>");
        html.writeln("</table>");

        super.writeFooter(html);
    }

    @Override
    protected boolean isMainIndex() {
        return true;
    }
}
