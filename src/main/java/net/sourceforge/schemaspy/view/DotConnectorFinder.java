package net.sourceforge.schemaspy.view;

import java.io.*;
import java.util.*;
import net.sourceforge.schemaspy.model.*;

/**
 * Format table data into .dot format to feed to GraphVis' dot program.
 *
 * @author John Currier
 */
public class DotConnectorFinder {
    private static DotConnectorFinder instance = new DotConnectorFinder();

    /**
     * Singleton - prevent creation
     */
    private DotConnectorFinder() {
    }

    public static DotConnectorFinder getInstance() {
        return instance;
    }

    /**
     *
     * @param table Table
     * @throws IOException
     * @return Set of <code>dot</code> relationships (as <code>DotEdge</code>s)
     */
    public Set getRelatedConnectors(Table table, WriteStats stats) {
        Set relationships = new HashSet();

        Iterator iter = table.getColumns().iterator();
        while (iter.hasNext()) {
            relationships.addAll(getRelatedConnectors((TableColumn)iter.next(), null, stats));
        }

        return relationships;
    }

    /**
     * Get all the relationships that exist between these two tables.
     *
     * @param table1 Table
     * @param table2 Table
     * @throws IOException
     * @return Set of <code>dot</code> relationships (as <code>DotEdge</code>s)
     */
    public Set getRelatedConnectors(Table table1, Table table2, WriteStats stats) {
        Set relationships = new HashSet();

        Iterator iter = table1.getColumns().iterator();
        while (iter.hasNext()) {
            relationships.addAll(getRelatedConnectors((TableColumn)iter.next(), table2, stats));
        }

        iter = table2.getColumns().iterator();
        while (iter.hasNext()) {
            relationships.addAll(getRelatedConnectors((TableColumn)iter.next(), table1, stats));
        }

        return relationships;
    }

    /**
     * @param column TableColumn
     * @param targetTable Table
     * @throws IOException
     * @return Set of <code>dot</code> relationships (as <code>DotEdge</code>s)
     */
    private Set getRelatedConnectors(TableColumn column, Table targetTable, WriteStats stats) {
        Set relatedConnectors = new HashSet();
        if (DotConnector.isExcluded(column, stats))
            return relatedConnectors;

        for (Iterator iter = column.getParents().iterator(); iter.hasNext(); ) {
            TableColumn parentColumn = (TableColumn)iter.next();
            Table parentTable = parentColumn.getTable();
            if (targetTable != null && parentTable != targetTable)
                continue;
            if (DotConnector.isExcluded(parentColumn, stats))
                continue;
            boolean implied = column.getParentConstraint(parentColumn).isImplied();
            if (stats.includeImplied() || !implied) {
                relatedConnectors.add(new DotConnector(parentColumn, column, implied));
            }
        }

        for (Iterator iter = column.getChildren().iterator(); iter.hasNext(); ) {
            TableColumn childColumn = (TableColumn)iter.next();
            Table childTable = childColumn.getTable();
            if (targetTable != null && childTable != targetTable)
                continue;
            if (DotConnector.isExcluded(childColumn, stats))
                continue;
            boolean implied = column.getChildConstraint(childColumn).isImplied();
            if (stats.includeImplied() || !implied) {
                relatedConnectors.add(new DotConnector(column, childColumn, implied));
            }
        }

        return relatedConnectors;
    }
}
