package net.sourceforge.schemaspy;

import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;
import net.sourceforge.schemaspy.model.*;

public class SchemaSpy {
    private final Database database;

    public SchemaSpy(Connection connection, DatabaseMetaData meta, String dbName, String schema, String description, Properties properties, Pattern include, int maxThreads) throws SQLException {
        database = new Database(connection, meta, dbName, schema, description, properties, include, maxThreads);
    }

    public Database getDatabase() {
        return database;
    }

    /**
     * Returns a list of <code>Table</code>s ordered such that parents are listed first
     * and child tables are listed last.
     *
     * <code>recursiveConstraints</code> gets populated with <code>TableConstraint</code>s
     * that had to be removed to resolve the returned list.
     *
     * @return
     */
    public List sortTablesByRI(Collection recursiveConstraints) {
        List heads = new ArrayList();
        List tails = new ArrayList();
        List remainingTables = new ArrayList(getDatabase().getTables());
        List unattached = new ArrayList();

        // first pass to gather the 'low hanging fruit'
        for (Iterator iter = remainingTables.iterator(); iter.hasNext(); ) {
            Table table = (Table)iter.next();
            if (table.isLeaf() && table.isRoot()) {
                unattached.add(table);
                iter.remove();
            }
        }

        unattached = sortTrimmedLevel(unattached);

        while (!remainingTables.isEmpty()) {
            int tablesLeft = remainingTables.size();
            tails.addAll(0, trimLeaves(remainingTables));
            heads.addAll(trimRoots(remainingTables));

            // if we could't trim anything then there's recursion....
            // resolve it by removing a constraint, one by one, 'till the tables are all trimmed
            if (tablesLeft == remainingTables.size()) {
                boolean foundSimpleRecursion = false;
                for (Iterator iter = remainingTables.iterator(); iter.hasNext(); ) {
                    Table potentialRecursiveTable = (Table)iter.next();
                    ForeignKeyConstraint recursiveConstraint = potentialRecursiveTable.removeSelfReferencingConstraint();
                    if (recursiveConstraint != null) {
                        recursiveConstraints.add(recursiveConstraint);
                        foundSimpleRecursion = true;
                    }
                }

                if (!foundSimpleRecursion) {
                    // expensive comparison, but we're down to the end of the tables so it shouldn't really matter
                    Set byParentChildDelta = new TreeSet(new Comparator() {
                        // sort on the delta between number of parents and kids so we can
                        // target the tables with the biggest delta and therefore the most impact
                        // on reducing the smaller of the two
                        public int compare(Object object1, Object object2) {
                            Table table1 = (Table)object1;
                            Table table2 = (Table)object2;
                            int rc = Math.abs(table2.getNumChildren() - table2.getNumParents()) - Math.abs(table1.getNumChildren() - table1.getNumParents());
                            if (rc == 0)
                                rc = table1.getName().compareTo(table2.getName());
                            return rc;
                        }
                    });
                    byParentChildDelta.addAll(remainingTables);
                    Table recursiveTable = (Table)byParentChildDelta.iterator().next(); // this one has the largest delta
                    ForeignKeyConstraint removedConstraint = recursiveTable.removeAForeignKeyConstraint();
                    recursiveConstraints.add(removedConstraint);
                }
            }
        }

        // we've gathered all the heads and tails, so combine them here moving 'unattached' tables to the end
        List ordered = new ArrayList(heads.size() + tails.size());
        for (Iterator iter = heads.iterator(); iter.hasNext(); )
            ordered.add(iter.next());
        heads = null; // allow gc ASAP
        
        for (Iterator iter = tails.iterator(); iter.hasNext(); )
            ordered.add(iter.next());
        tails = null; // allow gc ASAP
        
        for (Iterator iter = unattached.iterator(); iter.hasNext(); )
            ordered.add(iter.next());

        return ordered;
    }

    private static List trimRoots(List tables) {
        List roots = new ArrayList();

        Iterator iter = tables.iterator();
        while (iter.hasNext()) {
            Table root = (Table)iter.next();
            if (root.isRoot()) {
                roots.add(root);
                iter.remove();
            }
        }

        // now sort them so the ones with large numbers of children show up first (not required, but cool)
        roots = sortTrimmedLevel(roots);
        iter = roots.iterator();
        while (iter.hasNext()) {
            // do this after the previous loop to prevent getting roots before they're ready
            // and so we can sort them correctly
            ((Table)iter.next()).unlinkChildren();
        }

        return roots;
    }

    private static List trimLeaves(List tables) {
        List leaves = new ArrayList();

        Iterator iter = tables.iterator();
        while (iter.hasNext()) {
            Table leaf = (Table)iter.next();
            if (leaf.isLeaf()) {
                leaves.add(leaf);
                iter.remove();
            }
        }

        // now sort them so the ones with large numbers of children show up first (not required, but cool)
        leaves = sortTrimmedLevel(leaves);
        iter = leaves.iterator();
        while (iter.hasNext()) {
            // do this after the previous loop to prevent getting leaves before they're ready
            // and so we can sort them correctly
            ((Table)iter.next()).unlinkParents();
        }

        return leaves;
    }

    /**
     * this doesn't change the logical output of the program because all of these (leaves or roots) are at the same logical level
     */
    private static List sortTrimmedLevel(List tables) {
        /**
         * order by
         * <ul>
         *  <li>number of kids (descending)
         *  <li>number of parents (ascending)
         *  <li>alpha name (ascending)
         * </ul>
         */
        class TrimComparator implements Comparator {
            public int compare(Object object1, Object object2) {
                Table table1 = (Table)object1;
                Table table2 = (Table)object2;
                // have to keep track of and use the 'max' versions because
                // by the time we get here we'll (probably?) have no parents or children
                int rc = table2.getMaxChildren() - table1.getMaxChildren();
                if (rc == 0)
                    rc = table1.getMaxParents() - table2.getMaxParents();
                if (rc == 0)
                    rc = table1.getName().compareTo(table2.getName());
                return rc;
            }
        }

        TreeSet sorter = new TreeSet(new TrimComparator());
        sorter.addAll(tables);
        return new ArrayList(sorter);
    }
}
