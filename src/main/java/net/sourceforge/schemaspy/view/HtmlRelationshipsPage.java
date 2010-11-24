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

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.TableColumn;
import net.sourceforge.schemaspy.util.Dot;
import net.sourceforge.schemaspy.util.LineWriter;

/**
 * The page that contains the overview entity relationship diagrams.
 *
 * @author John Currier
 */
public class HtmlRelationshipsPage extends HtmlDiagramFormatter {
    private static final HtmlRelationshipsPage instance = new HtmlRelationshipsPage();
    private static final boolean fineEnabled = Logger.getLogger(HtmlRelationshipsPage.class.getName()).isLoggable(Level.FINE);

    /**
     * Singleton: Don't allow instantiation
     */
    private HtmlRelationshipsPage() {
    }

    /**
     * Singleton accessor
     *
     * @return the singleton instance
     */
    public static HtmlRelationshipsPage getInstance() {
        return instance;
    }

    public boolean write(Database db, File diagramDir, String dotBaseFilespec, boolean hasOrphans, boolean hasRealRelationships, boolean hasImpliedRelationships, Set<TableColumn> excludedColumns, LineWriter html) {
        File compactRelationshipsDotFile = new File(diagramDir, dotBaseFilespec + ".real.compact.dot");
        File compactRelationshipsDiagramFile = new File(diagramDir, dotBaseFilespec + ".real.compact.png");
        File largeRelationshipsDotFile = new File(diagramDir, dotBaseFilespec + ".real.large.dot");
        File largeRelationshipsDiagramFile = new File(diagramDir, dotBaseFilespec + ".real.large.png");
        File compactImpliedDotFile = new File(diagramDir, dotBaseFilespec + ".implied.compact.dot");
        File compactImpliedDiagramFile = new File(diagramDir, dotBaseFilespec + ".implied.compact.png");
        File largeImpliedDotFile = new File(diagramDir, dotBaseFilespec + ".implied.large.dot");
        File largeImpliedDiagramFile = new File(diagramDir, dotBaseFilespec + ".implied.large.png");

        try {
            Dot dot = getDot();
            if (dot == null) {
                writeHeader(db, null, "All Relationships", hasOrphans, html);
                html.writeln("<div class='content'>");
                writeInvalidGraphvizInstallation(html);
                html.writeln("</div>");
                writeFooter(html);
                return false;
            }

            writeHeader(db, "All Relationships", hasOrphans, hasRealRelationships, hasImpliedRelationships, html);
            html.writeln("<table width=\"100%\"><tr><td class=\"container\">");

            if (hasRealRelationships) {
                if (!fineEnabled)
                    System.out.print(".");

                html.writeln(dot.generateDiagram(compactRelationshipsDotFile, compactRelationshipsDiagramFile));
                html.writeln("  <a name='diagram'><img id='realCompactImg' src='diagrams/summary/" + compactRelationshipsDiagramFile.getName() + "' usemap='#compactRelationshipsDiagram' class='diagram' border='0' alt=''></a>");

                // we've run into instances where the first diagrams get generated, but then
                // dot fails on the second one...try to recover from that scenario 'somewhat'
                // gracefully
                try {
                    if (!fineEnabled)
                        System.out.print(".");

                    html.writeln(dot.generateDiagram(largeRelationshipsDotFile, largeRelationshipsDiagramFile));
                    html.writeln("  <a name='diagram'><img id='realLargeImg' src='diagrams/summary/" + largeRelationshipsDiagramFile.getName() + "' usemap='#largeRelationshipsDiagram' class='diagram' border='0' alt=''></a>");
                } catch (Dot.DotFailure dotFailure) {
                    System.err.println("dot failed to generate all of the relationships diagrams:");
                    System.err.println(dotFailure);
                    System.err.println("...but the relationships page may still be usable.");
                }
            }

            try {
                if (hasImpliedRelationships) {
                    if (!fineEnabled)
                        System.out.print(".");

                    html.writeln(dot.generateDiagram(compactImpliedDotFile, compactImpliedDiagramFile));
                    html.writeln("  <a name='diagram'><img id='impliedCompactImg' src='diagrams/summary/" + compactImpliedDiagramFile.getName() + "' usemap='#compactImpliedRelationshipsDiagram' class='diagram' border='0' alt=''></a>");

                    if (!fineEnabled)
                        System.out.print(".");

                    html.writeln(dot.generateDiagram(largeImpliedDotFile, largeImpliedDiagramFile));
                    html.writeln("  <a name='diagram'><img id='impliedLargeImg' src='diagrams/summary/" + largeImpliedDiagramFile.getName() + "' usemap='#largeImpliedRelationshipsDiagram' class='diagram' border='0' alt=''></a>");
                }
            } catch (Dot.DotFailure dotFailure) {
                System.err.println("dot failed to generate all of the relationships diagrams:");
                System.err.println(dotFailure);
                System.err.println("...but the relationships page may still be usable.");
            }

            if (!fineEnabled)
                System.out.print(".");
            html.writeln("</td></tr></table>");
            writeExcludedColumns(excludedColumns, null, html);

            writeFooter(html);
            return true;
        } catch (Dot.DotFailure dotFailure) {
            System.err.println(dotFailure);
            return false;
        } catch (IOException ioExc) {
            ioExc.printStackTrace();
            return false;
        }
    }

    private void writeHeader(Database db, String title, boolean hasOrphans, boolean hasRealRelationships, boolean hasImpliedRelationships, LineWriter html) throws IOException {
        writeHeader(db, null, title, hasOrphans, html);
        html.writeln("<table class='container' width='100%'>");
        html.writeln("<tr><td class='container'>");
        writeGeneratedBy(db.getConnectTime(), html);
        html.writeln("</td>");
        html.writeln("<td class='container' align='right' valign='top' rowspan='2'>");
        writeLegend(false, html);
        html.writeln("</td></tr>");
        if (!hasRealRelationships) {
            html.writeln("<tr><td class='container' align='left' valign='top'>");
            if (hasImpliedRelationships) {
                html.writeln("No 'real' Foreign Key relationships were detected in the schema.<br>");
                html.writeln("Displayed relationships are implied by a column's name/type/size matching another table's primary key.<p>");
            }
            else
                html.writeln("No relationships were detected in the schema.");
            html.writeln("</td></tr>");
        }
        html.writeln("<tr><td class='container' align='left' valign='top'>");

        html.writeln("<form name='options' action=''>");
        if (hasImpliedRelationships) {
            html.write("  <span ");
            // if no real relationships then hide the 'implied' checkbox and make it 'checked'
            if (!hasRealRelationships)
                html.write("style=\"display:none\" ");
            html.writeln("title=\"Show relationships implied by column name/type/size matching another table's primary key\">");
            html.write("    <label for='implied'><input type='checkbox' id='implied'" + (hasRealRelationships ? "" : " checked" ) + '>');
            html.writeln("Implied relationships</label>");
            html.writeln("  </span>");
        }
        if (hasRealRelationships || hasImpliedRelationships) {
            html.writeln("  <span title=\"By default only columns that are primary keys, foreign keys or indexes are shown\">");
            html.write("    <label for='showNonKeys'><input type='checkbox' id='showNonKeys'>");
            html.writeln("All columns</label>");
            html.writeln("  </span>");
        }
        html.writeln("</form>");

        html.writeln("</td></tr></table>");
    }

    @Override
    protected boolean isRelationshipsPage() {
        return true;
    }
}