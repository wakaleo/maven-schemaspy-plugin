package net.sourceforge.schemaspy.view;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import net.sourceforge.schemaspy.model.*;
import net.sourceforge.schemaspy.util.*;

/**
 * Format table data into .dot format to feed to GraphVis' dot program.
 *
 * @author John Currier
 */
public class DotFormatter {
    private static DotFormatter instance = new DotFormatter();
    private final int CompactGraphFontSize = 9;
    private final int LargeGraphFontSize = 11;
    private final String CompactNodeSeparator = "0.05";
    private final String CompactRankSeparator = "0.2";

    /**
     * Singleton - prevent creation
     */
    private DotFormatter() {
    }

    public static DotFormatter getInstance() {
        return instance;
    }

    /**
     * Write all relationships (including implied) associated with the given table
     */
    public void writeRealRelationships(Table table, boolean twoDegreesOfSeparation, WriteStats stats, LineWriter dot) throws IOException {
        boolean origImplied = stats.setIncludeImplied(false);
        writeRelationships(table, twoDegreesOfSeparation, stats, dot);
        stats.setIncludeImplied(origImplied);
    }

    /**
     * Write implied relationships associated with the given table
     */
    public void writeAllRelationships(Table table, boolean twoDegreesOfSeparation, WriteStats stats, LineWriter dot) throws IOException {
        boolean origImplied = stats.setIncludeImplied(true);
        writeRelationships(table, twoDegreesOfSeparation, stats, dot);
        stats.setIncludeImplied(origImplied);
    }

    /**
     * Write relationships associated with the given table
     */
    private void writeRelationships(Table table, boolean twoDegreesOfSeparation, WriteStats stats, LineWriter dot) throws IOException {
        Pattern regex = getRegexWithoutTable(table, stats);
        Pattern originalRegex = stats.setExclusionPattern(regex);
        Set tablesWritten = new HashSet();

        DotConnectorFinder finder = DotConnectorFinder.getInstance();

        String graphName = stats.includeImplied() ? "impliedTwoDegreesRelationshipsGraph" : (twoDegreesOfSeparation ? "twoDegreesRelationshipsGraph" : "oneDegreeRelationshipsGraph");
        writeHeader(graphName, false, true, dot);

        Set relatedTables = getImmediateRelatives(table, stats);

        Set connectors = new TreeSet(finder.getRelatedConnectors(table, stats));
        tablesWritten.add(table);

        Map nodes = new TreeMap();

        // write immediate relatives first
        Iterator iter = relatedTables.iterator();
        while (iter.hasNext()) {
            Table relatedTable = (Table)iter.next();
            if (!tablesWritten.add(relatedTable))
                continue; // already written

            nodes.put(relatedTable, new DotNode(relatedTable, true, ""));
            connectors.addAll(finder.getRelatedConnectors(relatedTable, table, stats));
        }

        // connect the edges that go directly to the target table
        // so they go to the target table's type column instead
        iter = connectors.iterator();
        while (iter.hasNext()) {
            DotConnector connector = (DotConnector)iter.next();
            if (connector.pointsTo(table))
                connector.connectToParentDetails();
        }

        Set allCousins = new HashSet();
        Set allCousinConnectors = new TreeSet();

        // next write 'cousins' (2nd degree of separation)
        if (twoDegreesOfSeparation) {
            iter = relatedTables.iterator();
            while (iter.hasNext()) {
                Table relatedTable = (Table)iter.next();
                Set cousins = getImmediateRelatives(relatedTable, stats);

                Iterator cousinsIter = cousins.iterator();
                while (cousinsIter.hasNext()) {
                    Table cousin = (Table)cousinsIter.next();
                    if (!tablesWritten.add(cousin))
                        continue; // already written

                    allCousinConnectors.addAll(finder.getRelatedConnectors(cousin, relatedTable, stats));
                    nodes.put(cousin, new DotNode(cousin, false, ""));
                }

                allCousins.addAll(cousins);
            }
        }

        markExcludedColumns(nodes, stats.getExcludedColumns());

        // now directly connect the loose ends to the title of the
        // 2nd degree of separation tables
        iter = allCousinConnectors.iterator();
        while (iter.hasNext()) {
            DotConnector connector = (DotConnector)iter.next();
            if (allCousins.contains(connector.getParentTable()))
                connector.connectToParentTitle();
            if (allCousins.contains(connector.getChildTable()))
                connector.connectToChildTitle();
        }

        // include the table itself
        nodes.put(table, new DotNode(table, ""));

        connectors.addAll(allCousinConnectors);
        iter = connectors.iterator();
        while (iter.hasNext()) {
            DotConnector connector = (DotConnector)iter.next();
            if (connector.isImplied()) {
                DotNode node = (DotNode)nodes.get(connector.getParentTable());
                if (node != null)
                    node.setShowImplied(true);
                node = (DotNode)nodes.get(connector.getChildTable());
                if (node != null)
                    node.setShowImplied(true);
            }
            dot.writeln(connector.toString());
        }

        iter = nodes.values().iterator();
        while (iter.hasNext()) {
            DotNode node = (DotNode)iter.next();
            dot.writeln(node.toString());
            stats.wroteTable(node.getTable());
        }

        dot.writeln("}");
        stats.setExclusionPattern(originalRegex);
    }

    private Pattern getRegexWithoutTable(Table table, WriteStats stats) {
        Set pieces = new HashSet();
        List regexes = Arrays.asList(stats.getExclusionPattern().pattern().split("\\)\\|\\("));
        for (int i = 0; i < regexes.size(); ++i) {
            String regex = regexes.get(i).toString();
            if (!regex.startsWith("("))
                regex = "(" + regex;
            if (!regex.endsWith(")"))
                regex = regex + ")";
            pieces.add(regex);
        }

        // now removed the pieces that match some of the columns of this table
        Iterator iter = pieces.iterator();
        while (iter.hasNext()) {
            String regex = iter.next().toString();
            Iterator columnIter = table.getColumns().iterator();
            while (columnIter.hasNext()) {
                TableColumn column = (TableColumn)columnIter.next();
                Pattern columnPattern = Pattern.compile(regex);
                if (column.matches(columnPattern)) {
                    iter.remove();
                    break;
                }
            }
        }

        StringBuffer pattern = new StringBuffer();
        iter = pieces.iterator();
        while (iter.hasNext()) {
            if (pattern.length() > 0)
                pattern.append("|");
            pattern.append(iter.next());
        }

        return Pattern.compile(pattern.toString());
    }

    private Set getImmediateRelatives(Table table, WriteStats stats) {
        Set relatedColumns = new HashSet();
        boolean foundImplied = false;
        Iterator iter = table.getColumns().iterator();
        while (iter.hasNext()) {
            TableColumn column = (TableColumn)iter.next();
            if (DotConnector.isExcluded(column, stats)) {
                stats.addExcludedColumn(column);
                continue;
            }
            Iterator childIter = column.getChildren().iterator();
            while (childIter.hasNext()) {
                TableColumn childColumn = (TableColumn)childIter.next();
                if (DotConnector.isExcluded(childColumn, stats)) {
                    stats.addExcludedColumn(childColumn);
                    continue;
                }
                boolean implied = column.getChildConstraint(childColumn).isImplied();
                foundImplied |= implied;
                if (!implied || stats.includeImplied())
                    relatedColumns.add(childColumn);
            }
            Iterator parentIter = column.getParents().iterator();
            while (parentIter.hasNext()) {
                TableColumn parentColumn = (TableColumn)parentIter.next();
                if (DotConnector.isExcluded(parentColumn, stats)) {
                    stats.addExcludedColumn(parentColumn);
                    continue;
                }
                boolean implied = column.getParentConstraint(parentColumn).isImplied();
                foundImplied |= implied;
                if (!implied || stats.includeImplied())
                    relatedColumns.add(parentColumn);
            }
        }

        Set relatedTables = new HashSet();
        iter = relatedColumns.iterator();
        while (iter.hasNext()) {
            TableColumn column = (TableColumn)iter.next();
            relatedTables.add(column.getTable());
        }

        relatedTables.remove(table);
        stats.setWroteImplied(foundImplied);
        return relatedTables;
    }

    private void writeHeader(String graphName, boolean compact, boolean showLabel, LineWriter dot) throws IOException {
        dot.writeln("// dot " + Dot.getInstance().getVersion() + " on " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        dot.writeln("digraph \"" + graphName + "\" {");
        dot.writeln("  graph [");
        boolean rankdirbug = Boolean.getBoolean("rankdirbug");  // another nasty hack
        if (!rankdirbug)
            dot.writeln("    rankdir=\"RL\"");
        dot.writeln("    bgcolor=\"" + StyleSheet.getInstance().getBodyBackground() + "\"");
        if (showLabel) {
            if (rankdirbug)
                dot.writeln("    label=\"\\nLayout is significantly better without '-rankdirbug' option\"");
            else
                dot.writeln("    label=\"\\nGenerated by SchemaSpy\"");
            dot.writeln("    labeljust=\"l\"");
        }
        if (compact) {
            dot.writeln("    nodesep=\"" + CompactNodeSeparator + "\"");
            dot.writeln("    ranksep=\"" + CompactRankSeparator + "\"");
        }
        dot.writeln("  ];");
        dot.writeln("  node [");
        dot.writeln("    fontname=\"Helvetica\"");
        dot.writeln("    fontsize=\"" + (compact ? CompactGraphFontSize : LargeGraphFontSize) + "\"");
        dot.writeln("    shape=\"plaintext\"");
        dot.writeln("  ];");
        dot.writeln("  edge [");
        dot.writeln("    arrowsize=\"0.8\"");
        dot.writeln("  ];");
}

    public void writeRealRelationships(Collection tables, boolean compact, boolean details, WriteStats stats, LineWriter dot) throws IOException {
        boolean oldImplied = stats.setIncludeImplied(false);
        writeRelationships(tables, compact, details, stats, dot);
        stats.setIncludeImplied(oldImplied);
    }

    public void writeAllRelationships(Collection tables, boolean compact, boolean details, WriteStats stats, LineWriter dot) throws IOException {
        boolean oldImplied = stats.setIncludeImplied(true);
        writeRelationships(tables, compact, details, stats, dot);
        stats.setIncludeImplied(oldImplied);
    }

    private void writeRelationships(Collection tables, boolean compact, boolean details, WriteStats stats, LineWriter dot) throws IOException {
        DotConnectorFinder finder = DotConnectorFinder.getInstance();
        String graphName;
        if (stats.includeImplied()) {
            if (compact)
                graphName = "compactImpliedRelationshipsGraph";
            else
                graphName = "largeImpliedRelationshipsGraph";
        } else {
            if (compact)
                graphName = "compactRelationshipsGraph";
            else
                graphName = "largeRelationshipsGraph";
        }
        writeHeader(graphName, compact, true, dot);

        Map nodes = new TreeMap();

        Iterator iter = tables.iterator();
        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            if (!table.isOrphan(stats.includeImplied())) {
                nodes.put(table, new DotNode(table, details, "tables/"));
            }
        }

        Set connectors = new TreeSet();

        iter = tables.iterator();
        while (iter.hasNext())
            connectors.addAll(finder.getRelatedConnectors((Table)iter.next(), stats));

        markExcludedColumns(nodes, stats.getExcludedColumns());

        iter = nodes.values().iterator();
        while (iter.hasNext()) {
            DotNode node = (DotNode)iter.next();
            Table table = node.getTable();

            dot.writeln(node.toString());
            stats.wroteTable(table);
            if (stats.includeImplied() && table.isOrphan(!stats.includeImplied())) {
                stats.setWroteImplied(true);
            }
        }

        iter = connectors.iterator();
        while (iter.hasNext())
            dot.writeln(iter.next().toString());

        dot.writeln("}");
    }

    private void markExcludedColumns(Map nodes, Set excludedColumns) {
        Iterator iter = excludedColumns.iterator();
        while (iter.hasNext()) {
            TableColumn column = (TableColumn)iter.next();
            DotNode node = (DotNode)nodes.get(column.getTable());
            if (node != null) {
                node.excludeColumn(column);
            }
        }
    }

    public void writeOrphan(Table table, LineWriter dot) throws IOException {
        writeHeader(table.getName(), false, false, dot);
        dot.writeln(new DotNode(table, true, "tables/").toString());
        dot.writeln("}");
    }
}
