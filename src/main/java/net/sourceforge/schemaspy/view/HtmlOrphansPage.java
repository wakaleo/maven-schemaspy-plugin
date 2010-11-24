package net.sourceforge.schemaspy.view;

import java.io.*;
import java.util.*;
import net.sourceforge.schemaspy.model.*;
import net.sourceforge.schemaspy.util.*;

public class HtmlOrphansPage extends HtmlGraphFormatter {
    private static HtmlOrphansPage instance = new HtmlOrphansPage();

    private HtmlOrphansPage() {
    }

    public static HtmlOrphansPage getInstance() {
        return instance;
    }

    public boolean write(Database db, List orphanTables, File graphDir, LineWriter html) throws IOException {
        Dot dot = getDot();
        if (dot == null)
            return false;

        Set orphansWithImpliedRelationships = new HashSet();
        Iterator iter = orphanTables.iterator();
        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            if (!table.isOrphan(true)){
                orphansWithImpliedRelationships.add(table);
            }
        }

        writeHeader(db, "Utility Tables Graph", !orphansWithImpliedRelationships.isEmpty(), html);

        html.writeln("<a name='graph'>");
        try {
            iter = orphanTables.iterator();
            while (iter.hasNext()) {
                Table table = (Table)iter.next();
                String dotBaseFilespec = table.getName();

                File dotFile = new File(graphDir, dotBaseFilespec + ".1degree.dot");
                File graphFile = new File(graphDir, dotBaseFilespec + ".1degree.png");

                LineWriter dotOut = new LineWriter(new FileOutputStream(dotFile));
                DotFormatter.getInstance().writeOrphan(table, dotOut);
                dotOut.close();
                try {
                    dot.generateGraph(dotFile, graphFile);
                } catch (Dot.DotFailure dotFailure) {
                    System.err.println(dotFailure);
                    return false;
                }

                html.write("  <img src='graphs/summary/" + graphFile.getName() + "' usemap='#" + table + "' border='0' alt='' align='top'");
                if (orphansWithImpliedRelationships.contains(table))
                    html.write(" class='impliedNotOrphan'");
                html.writeln(">");
            }

            iter = orphanTables.iterator();
            while (iter.hasNext()) {
                Table table = (Table)iter.next();
                String dotBaseFilespec = table.getName();

                File dotFile = new File(graphDir, dotBaseFilespec + ".1degree.dot");
                dot.writeMap(dotFile, html);
            }

            return true;
        } finally {
            html.writeln("</a>");
            writeFooter(html);
        }
    }

    private void writeHeader(Database db, String title, boolean hasImpliedRelationships, LineWriter html) throws IOException {
        writeHeader(db, null, title, true, html);
        html.writeln("<table class='container' width='100%'>");
        html.writeln("<tr><td class='container'>");
        writeGeneratedBy(db.getConnectTime(), html);
        html.writeln("</td>");
        html.writeln("<td class='container' align='right' valign='top' rowspan='2'>");
        writeLegend(false, html);
        html.writeln("</td></tr>");
        html.writeln("<tr><td class='container' align='left' valign='top'>");
        if (hasImpliedRelationships) {
            html.writeln("<form action=''>");
            html.writeln(" <input type=checkbox onclick=\"toggle(" + StyleSheet.getInstance().getOffsetOf(".impliedNotOrphan") + ");\" id=removeImpliedOrphans>");
            html.writeln("  Hide tables with implied relationships");
            html.writeln("</form>");
        }
        html.writeln("</td></tr></table>");
    }

    protected boolean isOrphansPage() {
        return true;
    }
}
