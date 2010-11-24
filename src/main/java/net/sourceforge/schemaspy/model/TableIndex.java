package net.sourceforge.schemaspy.model;

import java.sql.*;
import java.util.*;

public class TableIndex implements Comparable {
    private final String name;
    private final boolean isUnique;
    private Object id;
    private boolean isPrimary;
    private final List columns = new ArrayList();
    private final List columnsAscending = new ArrayList(); // Booleans for whether colums are ascending order

    public TableIndex(ResultSet rs) throws SQLException {
        name = rs.getString("INDEX_NAME");
        isUnique = !rs.getBoolean("NON_UNIQUE");
    }

    public void setId(Object id) {
        this.id = id;
    }

    public Object getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    void addColumn(TableColumn column, String sortOrder) {
        if (column != null) {
            columns.add(column);
            columnsAscending.add(Boolean.valueOf(sortOrder == null || sortOrder.equals("A")));
        }
    }

    public String getType() {
        if (isPrimaryKey())
            return "Primary key";
        if (isUnique())
            return "Must be unique";
        return "Performance";
    }

    public boolean isPrimaryKey() {
        return isPrimary;
    }

    public void setIsPrimaryKey(boolean isPrimaryKey) {
        isPrimary = isPrimaryKey;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public String getColumnsAsString() {
        StringBuffer buf = new StringBuffer();

        Iterator iter = columns.iterator();
        while (iter.hasNext()) {
            if (buf.length() > 0)
                buf.append(" + ");
            buf.append(iter.next());
        }
        return buf.toString();
    }

    public List getColumns() {
        return Collections.unmodifiableList(columns);
    }

    /**
     * Yes, we had a project that had columns defined as both 'nullable' and 'must be unique'.
     *
     * @return boolean
     */
    public boolean isUniqueNullable() {
        if (!isUnique())
            return false;

        // if all of the columns specified by the Unique Index are nullable
        // then return true, otherwise false
        boolean allNullable = true;
        for (Iterator iter = getColumns().iterator(); iter.hasNext() && allNullable; ) {
            TableColumn column = (TableColumn)iter.next();
            allNullable = column != null && column.isNullable();
        }

        return allNullable;
    }

    public boolean isAscending(TableColumn column) {
        return ((Boolean)columnsAscending.get(columns.indexOf(column))).booleanValue();
    }

    public int compareTo(Object object) {
        TableIndex other = (TableIndex)object;
        if (isPrimaryKey() && !other.isPrimaryKey())
            return -1;
        if (!isPrimaryKey() && other.isPrimaryKey())
            return 1;
        if (getId() == null)
            return getName().compareTo(other.getName());
        if (getId() instanceof Number)
            return ((Number)getId()).intValue() - ((Number)other.getId()).intValue();
        return getId().toString().compareTo(other.getId().toString());
    }
}
