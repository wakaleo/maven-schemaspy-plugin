package net.sourceforge.schemaspy.view;

import java.util.*;
import org.w3c.dom.*;
import net.sourceforge.schemaspy.model.*;
import net.sourceforge.schemaspy.util.*;

public class XmlTableFormatter {
    private static final XmlTableFormatter instance = new XmlTableFormatter();

    /**
     * Singleton...don't allow instantiation
     */
    private XmlTableFormatter() {}

    public static XmlTableFormatter getInstance() {
        return instance;
    }

    /**
     * Append the specified tables to the XML node
     * 
     * @param schemaNode
     * @param tables
     */
    public void appendTables(Element schemaNode, Collection tables) {
        Set byName = new TreeSet(new Comparator() {
            public int compare(Object object1, Object object2) {
                return ((Table)object1).getName().compareTo(((Table)object2).getName());
            }
        });
        byName.addAll(tables);

        Document document = schemaNode.getOwnerDocument();
        Element tablesNode = document.createElement("tables");
        schemaNode.appendChild(tablesNode);
        for (Iterator iter = byName.iterator(); iter.hasNext(); ) {
            appendTable(tablesNode, (Table)iter.next());
        }
    }
    
    private void appendTable(Element tablesNode, Table table) {
        Document document = tablesNode.getOwnerDocument();
        Element tableNode = document.createElement("table");
        tablesNode.appendChild(tableNode);
        if (table.getId() != null)
            DOMUtil.appendAttribute(tableNode, "id", String.valueOf(table.getId()));
        if (table.getSchema() != null)
            DOMUtil.appendAttribute(tableNode, "schema", table.getSchema());
        DOMUtil.appendAttribute(tableNode, "name", table.getName());
        DOMUtil.appendAttribute(tableNode, "numRows", String.valueOf(table.getNumRows()));
        DOMUtil.appendAttribute(tableNode, "type", table.isView() ? "VIEW" : "TABLE");
        DOMUtil.appendAttribute(tableNode, "remarks", table.getComments() == null ? "" : table.getComments());
        appendColumns(tableNode, table);
        appendPrimaryKeys(tableNode, table);
        appendIndexes(tableNode, table);
        appendCheckConstraints(tableNode, table);
        appendView(tableNode, table);
    }

    private void appendColumns(Element tableNode, Table table) {
        for (Iterator iter = table.getColumns().iterator(); iter.hasNext(); ) {
            appendColumn(tableNode, (TableColumn)iter.next());
        }
    }

    private Node appendColumn(Node tableNode, TableColumn column) {
        Document document = tableNode.getOwnerDocument();
        Node columnNode = document.createElement("column");
        tableNode.appendChild(columnNode);

        DOMUtil.appendAttribute(columnNode, "id", String.valueOf(column.getId()));
        DOMUtil.appendAttribute(columnNode, "name", column.getName());
        DOMUtil.appendAttribute(columnNode, "type", column.getType());
        DOMUtil.appendAttribute(columnNode, "size", String.valueOf(column.getLength()));
        DOMUtil.appendAttribute(columnNode, "digits", String.valueOf(column.getDecimalDigits()));
        DOMUtil.appendAttribute(columnNode, "nullable", String.valueOf(column.isNullable()));
        DOMUtil.appendAttribute(columnNode, "autoUpdated", String.valueOf(column.isAutoUpdated()));
        if (column.getDefaultValue() != null)
            DOMUtil.appendAttribute(columnNode, "defaultValue", String.valueOf(column.getDefaultValue()));
        DOMUtil.appendAttribute(columnNode, "remarks", column.getComments() == null ? "" : column.getComments());

        Iterator iter = column.getChildren().iterator();
        while (iter.hasNext()) {
            TableColumn childColumn = (TableColumn)iter.next();
            Node childNode = document.createElement("child");
            columnNode.appendChild(childNode);
            ForeignKeyConstraint constraint = column.getChildConstraint(childColumn);
            DOMUtil.appendAttribute(childNode, "foreignKey", constraint.getName());
            DOMUtil.appendAttribute(childNode, "table", childColumn.getTable().getName());
            DOMUtil.appendAttribute(childNode, "column", childColumn.getName());
            DOMUtil.appendAttribute(childNode, "implied", String.valueOf(constraint.isImplied()));
            DOMUtil.appendAttribute(childNode, "onDeleteCascade", String.valueOf(constraint.isOnDeleteCascade()));
        }

        iter = column.getParents().iterator();
        while (iter.hasNext()) {
            TableColumn parentColumn = (TableColumn)iter.next();
            Node parentNode = document.createElement("parent");
            columnNode.appendChild(parentNode);
            ForeignKeyConstraint constraint = column.getParentConstraint(parentColumn);
            DOMUtil.appendAttribute(parentNode, "foreignKey", constraint.getName());
            DOMUtil.appendAttribute(parentNode, "table", parentColumn.getTable().getName());
            DOMUtil.appendAttribute(parentNode, "column", parentColumn.getName());
            DOMUtil.appendAttribute(parentNode, "implied", String.valueOf(constraint.isImplied()));
            DOMUtil.appendAttribute(parentNode, "onDeleteCascade", String.valueOf(constraint.isOnDeleteCascade()));
        }

        return columnNode;
    }

    private void appendPrimaryKeys(Element tableNode, Table table) {
        Document document = tableNode.getOwnerDocument();
        int index = 1;
        
        for (Iterator iter = table.getPrimaryColumns().iterator(); iter.hasNext(); ) {
            TableColumn primaryKeyColumn = (TableColumn)iter.next();
            Node primaryKeyNode = document.createElement("primaryKey");
            tableNode.appendChild(primaryKeyNode);
            
            DOMUtil.appendAttribute(primaryKeyNode, "column", primaryKeyColumn.getName());
            DOMUtil.appendAttribute(primaryKeyNode, "sequenceNumberInPK", String.valueOf(index++));
        }
    }
    
    private void appendCheckConstraints(Element tableNode, Table table) {
        Document document = tableNode.getOwnerDocument();
        Map constraints = table.getCheckConstraints();
        if (constraints != null && !constraints.isEmpty()) {
            for (Iterator iter = constraints.keySet().iterator(); iter.hasNext(); ) {
                String name = iter.next().toString();
                Node constraintNode = document.createElement("checkConstraint");
                tableNode.appendChild(constraintNode);
                DOMUtil.appendAttribute(tableNode, "name", name);
                DOMUtil.appendAttribute(tableNode, "constraint", constraints.get(name).toString());
            }
        }
    }

    private void appendIndexes(Node tableNode, Table table) {
        boolean showId = table.getId() != null;
        Set indexes = table.getIndexes();
        if (indexes != null && !indexes.isEmpty()) {
            indexes = new TreeSet(indexes); // sort primary keys first
            Document document = tableNode.getOwnerDocument();

            for (Iterator iter = indexes.iterator(); iter.hasNext(); ) {
                TableIndex index = (TableIndex)iter.next();

                Node indexNode = document.createElement("index");
                if (showId)
                    DOMUtil.appendAttribute(indexNode, "id", String.valueOf(index.getId()));
                DOMUtil.appendAttribute(indexNode, "name", index.getName());
                DOMUtil.appendAttribute(indexNode, "unique", String.valueOf(index.isUnique()));
                Iterator columnsIter = index.getColumns().iterator();
                while (columnsIter.hasNext()) {
                    TableColumn column = (TableColumn)columnsIter.next();
                    Node columnNode = document.createElement("column");
                    DOMUtil.appendAttribute(columnNode, "name", column.getName());
                    DOMUtil.appendAttribute(columnNode, "ascending", String.valueOf(index.isAscending(column)));
                    indexNode.appendChild(columnNode);
                }
                tableNode.appendChild(indexNode);
            }
        }
    }

    private void appendView(Element tableNode, Table table) {
        String sql;
        if (table.isView() && (sql = table.getViewSql()) != null) {
            DOMUtil.appendAttribute(tableNode, "viewSql", sql);
        }
    }
}
