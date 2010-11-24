package net.sourceforge.schemaspy;

import java.sql.*;
import java.util.*;
import java.util.regex.*;
import net.sourceforge.schemaspy.model.*;

public class DBAnalyzer {
    public static List getImpliedConstraints(Collection tables) throws SQLException {
        List columnsWithoutParents = new ArrayList();
        Map allPrimaries = new TreeMap(new Comparator() {
            public int compare(Object object1, Object object2) {
                TableColumn column1 = (TableColumn)object1;
                TableColumn column2 = (TableColumn)object2;
                int rc = column1.getName().compareTo(column2.getName());
                if (rc == 0)
                    rc = column1.getType().compareTo(column2.getType());
                if (rc == 0)
                    rc = column1.getLength() - column2.getLength();
                return rc;
            }
        });

        int duplicatePrimaries = 0;

        // gather all the primary key columns and columns without parents
        for (Iterator iter = tables.iterator(); iter.hasNext(); ) {
            Table table = (Table)iter.next();
            List tablePrimaries = table.getPrimaryColumns();
            if (tablePrimaries.size() == 1) { // can't match up multiples...yet...
                for (Iterator primariesIter = tablePrimaries.iterator(); primariesIter.hasNext(); ) {
                    if (allPrimaries.put(primariesIter.next(), table) != null)
                        ++duplicatePrimaries;
                }
            }

            for (Iterator columnIter = table.getColumns().iterator(); columnIter.hasNext(); ) {
                TableColumn column = (TableColumn)columnIter.next();
                if (column.getParents().isEmpty())
                    columnsWithoutParents.add(column);
            }
        }

        // if more than half of the tables have the same primary key then
        // it's most likey a database where primary key names aren't unique
        // (e.g. they all have a primary key named 'ID')
        if (duplicatePrimaries > allPrimaries.size()) // bizarre logic, but it does approximately what we need
            return new ArrayList();

        sortColumnsByTable(columnsWithoutParents);

        List impliedConstraints = new ArrayList();
        for (Iterator iter = columnsWithoutParents.iterator(); iter.hasNext(); ) {
            TableColumn childColumn = (TableColumn)iter.next();
            Table primaryTable = (Table)allPrimaries.get(childColumn);
            if (primaryTable != null && primaryTable != childColumn.getTable()) {
                TableColumn parentColumn = primaryTable.getColumn(childColumn.getName());
                // make sure the potential child->parent relationships isn't already a
                // parent->child relationship
                if (parentColumn.getParentConstraint(childColumn) == null) {
                    // ok, we've found a potential relationship with a column matches a primary
                    // key column in another table and isn't already related to that column
                    impliedConstraints.add(new ImpliedForeignKeyConstraint(parentColumn, childColumn));
                }
            }
        }

        return impliedConstraints;
    }

    /**
     * Returns a <code>List</code> of all of the <code>ForeignKeyConstraint</code>s
     * used by the specified tables.
     *
     * @param tables Collection
     * @return List
     */
    public static List getForeignKeyConstraints(Collection tables) {
        List constraints = new ArrayList();
        Iterator iter = tables.iterator();
        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            constraints.addAll(table.getForeignKeys());
        }

        return constraints;
    }

    public static List getOrphans(Collection tables) {
        List orphans = new ArrayList();

        Iterator iter = tables.iterator();
        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            if (table.isOrphan(false)) {
                orphans.add(table);
            }
        }

        return sortTablesByName(orphans);
    }

    /**
     * Return a list of <code>TableColumn</code>s that are both nullable
     * and have an index that specifies that they must be unique (a rather strange combo).
     */
    public static List getMustBeUniqueNullableColumns(Collection tables) {
        List uniqueNullables = new ArrayList();

        for (Iterator tablesIter = tables.iterator(); tablesIter.hasNext(); ) {
            Table table = (Table)tablesIter.next();
            for (Iterator indexesIter = table.getIndexes().iterator(); indexesIter.hasNext(); ) {
                TableIndex index = (TableIndex)indexesIter.next();
                if (index.isUniqueNullable()) {
                    uniqueNullables.addAll(index.getColumns());
                }
            }
        }

        return sortColumnsByTable(uniqueNullables);
    }

    /**
     * Return a list of <code>Table</code>s that have neither an index nor a primary key.
     */
    public static List getTablesWithoutIndexes(Collection tables) {
        List withoutIndexes = new ArrayList();

        for (Iterator tablesIter = tables.iterator(); tablesIter.hasNext(); ) {
            Table table = (Table)tablesIter.next();
            if (!table.isView() && table.getIndexes().size() == 0)
                withoutIndexes.add(table);
        }

        return sortTablesByName(withoutIndexes);
    }

    public static List getTablesWithIncrementingColumnNames(Collection tables) {
        List denormalizedTables = new ArrayList();

        Iterator tableIter = tables.iterator();
        while (tableIter.hasNext()) {
            Table table = (Table)tableIter.next();
            Map columnPrefixes = new HashMap();

            Iterator columnIter = table.getColumns().iterator();
            while (columnIter.hasNext()) {
                // search for columns that start with the same prefix
                // and end in an incrementing number
                TableColumn column = (TableColumn)columnIter.next();

                String columnName = column.getName();
                String numbers = null;
                for (int i = columnName.length() - 1; i > 0; --i) {
                    if (Character.isDigit(columnName.charAt(i))) {
                        numbers = String.valueOf(columnName.charAt(i)) + (numbers == null ? "" : numbers);
                    } else {
                        break;
                    }
                }

                // attempt to detect where they had an existing column
                // and added a "column2" type of column (we'll call this one "1")
                if (numbers == null) {
                    numbers = "1";
                    columnName = columnName + numbers;
                }

                // see if we've already found a column with the same prefix
                // that had a numeric suffix +/- 1.
                String prefix = columnName.substring(0, columnName.length() - numbers.length());
                long numeric = Long.parseLong(numbers);
                Long existing = (Long)columnPrefixes.get(prefix);
                if (existing != null && Math.abs(existing.longValue() - numeric) == 1) {
                    // found one so add it to our list and stop evaluating this table
                    denormalizedTables.add(table);
                    break;
                }
                columnPrefixes.put(prefix, new Long(numeric));
            }
        }

        return sortTablesByName(denormalizedTables);
    }

    public static List getTablesWithOneColumn(Collection tables) {
        List singleColumnTables = new ArrayList();

        Iterator iter = tables.iterator();
        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            if (table.getColumns().size() == 1)
                singleColumnTables.add(table);
        }

        return sortTablesByName(singleColumnTables);
    }

    public static List sortTablesByName(List tables) {
        Collections.sort(tables, new Comparator() {
            public int compare(Object object1, Object object2) {
                return ((Table)object1).getName().compareTo(((Table)object2).getName());
            }
        });

        return tables;
    }

    public static List sortColumnsByTable(List columns) {
        Collections.sort(columns, new Comparator() {
            public int compare(Object object1, Object object2) {
                TableColumn column1 = (TableColumn)object1;
                TableColumn column2 = (TableColumn)object2;
                int rc = column1.getTable().getName().compareTo(column2.getTable().getName());
                if (rc == 0)
                    rc = column1.getName().compareTo(column2.getName());
                return rc;
            }
        });

        return columns;
    }

    /**
     * Returns a list of columns that have the word "NULL" or "null" as their default value
     * instead of the likely candidate value null.
     *
     * @param tables Collection
     * @return List
     */
    public static List getDefaultNullStringColumns(Collection tables) {
        List defaultNullStringColumns = new ArrayList();

        for (Iterator tablesIter = tables.iterator(); tablesIter.hasNext(); ) {
            Table table = (Table)tablesIter.next();
            for (Iterator columnsIter = table.getColumns().iterator(); columnsIter.hasNext(); ) {
                TableColumn column = (TableColumn)columnsIter.next();
                Object defaultValue = column.getDefaultValue();
                if (defaultValue != null && defaultValue instanceof String) {
                    String defaultString = defaultValue.toString();
                    if (defaultString.trim().equalsIgnoreCase("null")) {
                        defaultNullStringColumns.add(column);
                    }
                }
            }
        }

        return sortColumnsByTable(defaultNullStringColumns);
    }

    /**
     * getSchemas - returns a List of schema names (Strings)
     *
     * @param meta DatabaseMetaData
     */
    public static List getSchemas(DatabaseMetaData meta) throws SQLException {
        List schemas = new ArrayList();

        ResultSet rs = meta.getSchemas();
        while (rs.next()) {
            schemas.add(rs.getString("TABLE_SCHEM"));
        }
        rs.close();

        return schemas;
    }

    /**
     * getSchemas - returns a List of schema names (Strings) that contain tables
     *
     * @param meta DatabaseMetaData
     */
    public static List getPopulatedSchemas(DatabaseMetaData meta) throws SQLException {
        return getPopulatedSchemas(meta, ".*");
    }

    /**
     * getSchemas - returns a List of schema names (Strings) that contain tables and
     * match the <code>schemaSpec</code> regular expression
     *
     * @param meta DatabaseMetaData
     */
    public static List getPopulatedSchemas(DatabaseMetaData meta, String schemaSpec) throws SQLException {
        Set schemas = new TreeSet(); // alpha sorted
        Pattern schemaRegex = Pattern.compile(schemaSpec);

        Iterator iter = getSchemas(meta).iterator();
        while (iter.hasNext()) {
            String schema = iter.next().toString();
            if (schemaRegex.matcher(schema).matches()) {
                ResultSet rs = null;
                try {
                    rs = meta.getTables(null, schema, "%", null);
                    if (rs.next())
                        schemas.add(schema);
                } catch (SQLException ignore) {
                } finally {
                    if (rs != null)
                        rs.close();
                }
            }
        }

        return new ArrayList(schemas);
    }

    /**
     * For debugging/analyzing result sets
     * @param rs ResultSet
     * @throws SQLException
     */
    public static void dumpResultSetRow(ResultSet rs, String description) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int numColumns = meta.getColumnCount();
        System.out.println(numColumns + " columns of " + description + ":");
        for (int i = 1; i <= numColumns; ++i) {
            System.out.print(meta.getColumnLabel(i));
            System.out.print(": ");
            System.out.print(String.valueOf(rs.getString(i)));
            System.out.print("\t");
        }
        System.out.println();
    }
}
