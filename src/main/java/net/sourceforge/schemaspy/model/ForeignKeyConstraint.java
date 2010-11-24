package net.sourceforge.schemaspy.model;

import java.sql.*;
import java.util.*;

public class ForeignKeyConstraint {
    private final String name;
    private Table parentTable;
    private final List parentColumns = new ArrayList();
    private final Table childTable;
    private final List childColumns = new ArrayList();
    private final char deleteRule;
    private final char updateRule;

    ForeignKeyConstraint(Table child, ResultSet rs) throws SQLException {
        if (rs != null)
            name = rs.getString("FK_NAME");
        else
            name = null; // implied constraints will have a null rs and override getName()
        childTable = child;
        deleteRule = 'D';
        updateRule = 'U';
    }

    void addParentColumn(TableColumn column) {
        if (column != null) {
            parentColumns.add(column);
            parentTable = column.getTable();
        }
    }

    void addChildColumn(TableColumn column) {
        if (column != null)
            childColumns.add(column);
    }

    public String getName() {
        return name;
    }

    public Table getParentTable() {
        return parentTable;
    }

    public List getParentColumns() {
        return Collections.unmodifiableList(parentColumns);
    }

    public Table getChildTable() {
        return childTable;
    }

    public List getChildColumns() {
        return Collections.unmodifiableList(childColumns);
    }

    public char getDeleteRule() {
        return deleteRule;
    }

    public boolean isOnDeleteCascade() {
        return deleteRule == 'C';
    }

    public char getUpdateRule() {
        return updateRule;
    }

    public boolean isImplied() {
        return false;
    }

    public static String toString(List columns) {
        if (columns.size() == 1)
            return columns.iterator().next().toString();
        return columns.toString();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(childTable.getName());
        buf.append('.');
        buf.append(toString(childColumns));
        buf.append(" refs ");
        buf.append(parentTable.getName());
        buf.append('.');
        buf.append(toString(parentColumns));
        buf.append(" via ");
        buf.append(name);

        return buf.toString();
    }
}
