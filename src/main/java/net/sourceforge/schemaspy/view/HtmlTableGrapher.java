package net.sourceforge.schemaspy.view;

import net.sourceforge.schemaspy.model.Table;
import java.io.File;
import net.sourceforge.schemaspy.util.LineWriter;
import net.sourceforge.schemaspy.util.Dot;
import java.io.IOException;

public class HtmlTableGrapher extends HtmlGraphFormatter {
    private static HtmlTableGrapher instance = new HtmlTableGrapher();

    private HtmlTableGrapher() {
    }

    public static HtmlTableGrapher getInstance() {
        return instance;
    }

    public boolean write(Table table, File graphDir, WriteStats stats, LineWriter html) {
        File oneDegreeDotFile = new File(graphDir, table.getName() + ".1degree.dot");
        File oneDegreeGraphFile = new File(graphDir, table.getName() + ".1degree.png");
        File impliedDotFile = new File(graphDir, table.getName() + ".implied2degrees.dot");
        File impliedGraphFile = new File(graphDir, table.getName() + ".implied2degrees.png");
        File twoDegreesDotFile = new File(graphDir, table.getName() + ".2degrees.dot");
        File twoDegreesGraphFile = new File(graphDir, table.getName() + ".2degrees.png");

        try {
            Dot dot = getDot();
            if (dot == null)
                return false;

            dot.generateGraph(oneDegreeDotFile, oneDegreeGraphFile);

            html.write("<br/><form action='get'><b>Close relationships");
            if (stats.wroteTwoDegrees()) {
                html.writeln("</b><span class='degrees' id='degrees'>");
                html.write("&nbsp;within <input type='radio' name='degrees' id='oneDegree' onclick=\"");
                html.write("if (!this.checked)");
                html.write(" selectGraph('../graphs/" + twoDegreesGraphFile.getName() + "', '#twoDegreesRelationshipsGraph');");
                html.write("else");
                html.write(" selectGraph('../graphs/" + oneDegreeGraphFile.getName() + "', '#oneDegreeRelationshipsGraph'); ");
                html.writeln("\" checked>one");
                html.write("  <input type='radio' name='degrees' id='twoDegrees' onclick=\"");
                html.write("if (this.checked)");
                html.write(" selectGraph('../graphs/" + twoDegreesGraphFile.getName() + "', '#twoDegreesRelationshipsGraph');");
                html.write("else");
                html.write(" selectGraph('../graphs/" + oneDegreeGraphFile.getName() + "', '#oneDegreeRelationshipsGraph'); ");
                html.writeln("\">two degrees of separation");
                html.write("</span><b>:</b>");
                html.writeln("</form>");
            } else {
                html.write(":</b></form>");
            }
            html.writeln("  <a name='graph'><img src='../graphs/" + oneDegreeGraphFile.getName() + "' usemap='#oneDegreeRelationshipsGraph' id='relationships' border='0' alt='' align='left'></a>");
            dot.writeMap(oneDegreeDotFile, html);
            if (stats.wroteImplied()) {
                dot.generateGraph(impliedDotFile, impliedGraphFile);
                dot.writeMap(impliedDotFile, html);
            } else {
                impliedDotFile.delete();
                impliedGraphFile.delete();
            }
            if (stats.wroteTwoDegrees()) {
                dot.generateGraph(twoDegreesDotFile, twoDegreesGraphFile);
                dot.writeMap(twoDegreesDotFile, html);
            } else {
                twoDegreesDotFile.delete();
                twoDegreesGraphFile.delete();
            }
        } catch (Dot.DotFailure dotFailure) {
            System.err.println(dotFailure);
            return false;
        } catch (IOException ioExc) {
            ioExc.printStackTrace();
            return false;
        }

        return true;
    }
}
