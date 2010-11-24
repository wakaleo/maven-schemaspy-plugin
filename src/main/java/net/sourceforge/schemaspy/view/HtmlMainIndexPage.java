package net.sourceforge.schemaspy.view;

import java.io.*;
import java.text.*;
import java.util.*;
import net.sourceforge.schemaspy.model.*;
import net.sourceforge.schemaspy.util.*;

public class HtmlMainIndexPage extends HtmlFormatter {
    private static HtmlMainIndexPage instance = new HtmlMainIndexPage();
    private NumberFormat integerFormatter = NumberFormat.getIntegerInstance();

    /**
     * Singleton - prevent creation
     */
    private HtmlMainIndexPage() {
    }

    public static HtmlMainIndexPage getInstance() {
        return instance;
    }

    public void write(Database database, Collection tables, boolean showOrphansGraph, LineWriter html) throws IOException {
        Set byName = new TreeSet(new Comparator() {
            public int compare(Object object1, Object object2) {
                return ((Table)object1).getName().compareTo(((Table)object2).getName());
            }
        });
        byName.addAll(tables);

        boolean showIds = false;
        int numViews = 0;
        
        for (Iterator iter = byName.iterator(); iter.hasNext(); ) {
            Table table = (Table)iter.next();
            if (table.isView())
                ++numViews;
            showIds |= table.getId() != null;
        }

        writeHeader(database, byName.size() - numViews, numViews, showIds, showOrphansGraph, html);

        int numRows = 0;
        for (Iterator iter = byName.iterator(); iter.hasNext(); ) {
            Table table = (Table)iter.next();
            numRows += writeLineItem(table, showIds, html);
        }

        writeFooter(numRows, html);
    }

    private void writeHeader(Database db, int numberOfTables, int numberOfViews, boolean showIds, boolean hasOrphans, LineWriter html) throws IOException {
        writeHeader(db, null, null, hasOrphans, html);
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
        html.write("    <br/>");
        writeFeedMe(html);
        html.writeln("  </td>");
        html.writeln(" </tr>");
        html.writeln(" <tr>");
        html.write("  <td class='container'>");
        String xmlName = db.getName();
        if (db.getSchema() != null)
            xmlName += '.' + db.getSchema();
        html.write("<br/><a href='" + xmlName + ".xml' title='XML Representation'>XML Representation</a>");
        html.write("<br/><a href='insertionOrder.txt' title='Useful for loading data into a database'>Insertion Order</a>&nbsp;");
        html.write("<a href='deletionOrder.txt' title='Useful for purging data from a database'>Deletion Order</a>");
        html.write("&nbsp;(for database loading/purging scripts)");
        html.writeln("</td>");
        html.writeln(" </tr>");
        html.writeln("</table>");

        html.writeln("<div class='indent'>");
        html.write("<p/><b>");
        html.write(String.valueOf(numberOfTables));
        html.write(" Tables");
        if (numberOfViews > 0) {
            html.write(" and ");
            html.write(String.valueOf(numberOfViews));
            html.write(" View");
            if (numberOfViews != 1)
                html.write("s");
        }
        html.writeln(":</b>");
        html.writeln("<TABLE class='dataTable' border='1' rules='groups'>");
        int numGroups = 4 + (showIds ? 1 : 0) + (displayTableComments ? 1 : 0);
        for (int i = 0; i < numGroups; ++i)
            html.writeln("<colgroup>");
        html.writeln("<thead align='left'>");
        html.writeln("<tr>");
        html.writeln("  <th valign='bottom'>Table</th>");
        if (showIds)
            html.writeln("  <th align='center' valign='bottom'>ID</th>");
        html.writeln("  <th align='right' valign='bottom'>Children</th>");
        html.writeln("  <th align='right' valign='bottom'>Parents</th>");
        html.writeln("  <th align='right' valign='bottom'>Rows</th>");
        if (displayTableComments)
            html.writeln("  <th align='left' valign='bottom'>Comments</th>");
        html.writeln("</tr>");
        html.writeln("</thead>");
        html.writeln("<tbody>");
    }

    private int writeLineItem(Table table, boolean showIds, LineWriter html) throws IOException {
        html.writeln(" <tr valign='top'>");
        html.write("  <td class='detail'><a href='tables/");
        html.write(table.getName());
        html.write(".html'>");
        html.write(table.getName());
        html.writeln("</a></td>");

        if (showIds) {
            html.write("  <td class='detail' align='right'>");
            html.write(String.valueOf(table.getId()));
            html.writeln("</td>");
        }

        html.write("  <td class='detail' align='right'>");
        int numRelatives = table.getNumRealChildren();
        if (numRelatives != 0)
            html.write(String.valueOf(integerFormatter.format(numRelatives)));
        html.writeln("</td>");
        html.write("  <td class='detail' align='right'>");
        numRelatives = table.getNumRealParents();
        if (numRelatives != 0)
            html.write(String.valueOf(integerFormatter.format(numRelatives)));
        html.writeln("</td>");

        html.write("  <td class='detail' align='right'>");
        if (!table.isView())
            html.write(String.valueOf(integerFormatter.format(table.getNumRows())));
        else
            html.write("<span title='Views contain no real rows'>view</span>");
        html.writeln("</td>");
        if (displayTableComments) {
            html.write("  <td class='detail'>");
            String comments = table.getComments();
            if (comments != null) {
                if (encodeComments)
                    for (int i = 0; i < comments.length(); ++i)
                        html.write(HtmlEncoder.encode(comments.charAt(i)));
                else
                    html.write(comments);
            }
            html.writeln("</td>");
        }
        html.writeln("  </tr>");

        return table.getNumRows();
    }

    protected void writeFooter(int numRows, LineWriter html) throws IOException {
        html.writeln("</TABLE>");
        html.write("<p/>Total rows: ");
        html.write(String.valueOf(integerFormatter.format(numRows)));
        html.writeln("</div>");
        super.writeFooter(html);
    }

    protected boolean isMainIndex() {
        return true;
    }
}
