package net.sourceforge.schemaspy.view;

import java.io.*;
import java.util.*;
import net.sourceforge.schemaspy.model.*;
import net.sourceforge.schemaspy.util.*;

public class HtmlColumnsPage extends HtmlFormatter {
    private static HtmlColumnsPage instance = new HtmlColumnsPage();

    /**
     * Singleton - prevent creation
     */
    private HtmlColumnsPage() {
    }

    public static HtmlColumnsPage getInstance() {
        return instance;
    }
    
    /**
     * Returns details about the columns that are displayed on this page.
     * 
     * @return
     */
    public List getColumnInfos()
    {
        List columns = new ArrayList();
        
        columns.add(new ColumnInfo("Column", new ByColumnComparator()));
        columns.add(new ColumnInfo("Table", new ByTableComparator()));
        columns.add(new ColumnInfo("Type", new ByTypeComparator()));
        columns.add(new ColumnInfo("Size", new BySizeComparator()));
        columns.add(new ColumnInfo("Nulls", new ByNullableComparator()));
        columns.add(new ColumnInfo("Auto", new ByAutoUpdateComparator()));
        columns.add(new ColumnInfo("Default", new ByDefaultValueComparator()));
        columns.add(new ColumnInfo("Children", new ByChildrenComparator()));
        columns.add(new ColumnInfo("Parents", new ByParentsComparator()));
        
        return columns;
    }
    
    public class ColumnInfo
    {
        private final String columnName;
        private final Comparator comparator;
        
        private ColumnInfo(String columnName, Comparator comparator)
        {
            this.columnName = columnName;
            this.comparator = comparator;
        }
        
        public String getColumnName() {
            return columnName;
        }
        
        public String getLocation() {
            return getLocation(columnName);
        }
        
        public String getLocation(String columnName) {
            return "columns.by" + columnName + ".html";
        }

        private Comparator getComparator() {
            return comparator;
        }
        
        public String toString() {
            return getLocation();
        }
    }

    public void write(Database database, Collection tables, ColumnInfo columnInfo, boolean showOrphansGraph, LineWriter html) throws IOException {
        Set columns = new TreeSet(columnInfo.getComparator());
        Set primaryColumns = new HashSet();
        Set indexedColumns = new HashSet();

        Iterator iter = tables.iterator();
        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            columns.addAll(table.getColumns());

            primaryColumns.addAll(table.getPrimaryColumns());
            Iterator indexIter = table.getIndexes().iterator();
            while (indexIter.hasNext()) {
                TableIndex index = (TableIndex)indexIter.next();
                indexedColumns.addAll(index.getColumns());
            }
        }
        
        writeHeader(database, columns.size(), showOrphansGraph, columnInfo, html);

        HtmlTablePage formatter = HtmlTablePage.getInstance();

        iter = columns.iterator();
        while (iter.hasNext()) {
            TableColumn column = (TableColumn)iter.next();
            formatter.writeColumn(column, column.getTable().getName(), primaryColumns, indexedColumns, false, false, html);
        }

        writeFooter(html);
    }

    private void writeHeader(Database db, int numberOfColumns, boolean hasOrphans, ColumnInfo selectedColumn, LineWriter html) throws IOException {
        writeHeader(db, null, "Columns", hasOrphans, html);

        html.writeln("<table width='100%' border='0'>");
        html.writeln("<tr><td class='container'>");
        writeGeneratedBy(db.getConnectTime(), html);
        html.writeln("</td><td class='container' rowspan='2' align='right' valign='top'>");
        writeLegend(false, false, html);
        html.writeln("</td></tr>");
        html.writeln("<tr valign='top'><td class='container' align='left' valign='top'>");
        html.writeln("<p/>");
        StyleSheet css = StyleSheet.getInstance();
        String commentStatus = Boolean.getBoolean("commentsInitiallyDisplayed") ? "checked " : "";
        html.writeln("<form name='options' action=''>");
        html.writeln(" <input type=checkbox onclick=\"toggle(" + css.getOffsetOf(".relatedKey") + ");\" id=showRelatedCols>Related columns");
        html.writeln(" <input type=checkbox onclick=\"toggle(" + css.getOffsetOf(".constraint") + ");\" id=showConstNames>Constraint names");
        html.writeln(" <input type=checkbox " + commentStatus + "onclick=\"toggle(" + css.getOffsetOf(".comment") + ");\" id=showComments>Comments");
        html.writeln(" <input type=checkbox checked onclick=\"toggle(" + css.getOffsetOf(".legend") + ");\" id=showLegend>Legend");
        html.writeln("</form>");
        html.writeln("</table>");

        html.writeln("<div class='indent'>");
        html.write("<b>");
        html.write(db.getName());
        if (db.getSchema() != null) {
            html.write('.');
            html.write(db.getSchema());
        }
        html.write(" contains ");
        html.write(String.valueOf(numberOfColumns));
        html.write(" columns:</b>");
        Collection tables = db.getTables();
        boolean hasTableIds = tables.size() > 0 && ((Table)tables.iterator().next()).getId() != null;
        writeMainTableHeader(hasTableIds, selectedColumn, html);
        html.writeln("<tbody valign='top'>");
    }

    public void writeMainTableHeader(boolean hasTableIds, ColumnInfo selectedColumn, LineWriter out) throws IOException {
        boolean showTableName = selectedColumn != null;
        out.writeln("<a name='columns'/>");
        out.writeln("<table class='dataTable' border='1' rules='groups'>");
        int span = 6;
        if (hasTableIds || showTableName)
            ++span;
        out.writeln("<colgroup span='" + span + "'>");
        out.writeln("<colgroup>");
        out.writeln("<colgroup>");
        out.writeln("<colgroup class='comment'>");

        out.writeln("<thead align='left'>");
        out.writeln("<tr>");
        if (hasTableIds && !showTableName)
            out.writeln(getTH(selectedColumn, "ID", null, "right"));
        out.writeln(getTH(selectedColumn, "Column", null, null));
        if (showTableName)
            out.writeln(getTH(selectedColumn, "Table", null, null));
        out.writeln(getTH(selectedColumn, "Type", null, null));
        out.writeln(getTH(selectedColumn, "Size", null, null));
        out.writeln(getTH(selectedColumn, "Nulls", "Are nulls allowed?", null));
        out.writeln(getTH(selectedColumn, "Auto", "Is column automatically updated?", null));
        out.writeln(getTH(selectedColumn, "Default", "Default value", null));
        out.writeln(getTH(selectedColumn, "Children", "Columns in tables that reference this column", null));
        out.writeln(getTH(selectedColumn, "Parents", "Columns in tables that are referenced by this column", null));
        out.writeln("  <th title='Comments' class='comment'><span class='notSortedByColumn'>Comments</span></th>");
        out.writeln("</tr>");
        out.writeln("</thead>");
    }
   
    private String getTH(ColumnInfo selectedColumn, String columnName, String title, String align) {
        StringBuffer buf = new StringBuffer("  <th");
        
        if (align != null) {
            buf.append(" align='");
            buf.append(align);
            buf.append("'");
        }
        
        if (title != null) {
            buf.append(" title='");
            buf.append(title);
            buf.append("'");
        }
        
        if (selectedColumn != null) {
            if (selectedColumn.getColumnName().equals(columnName)) {
                buf.append(" class='sortedByColumn'>");
                buf.append(columnName);
            } else {
                buf.append(" class='notSortedByColumn'>");
                buf.append("<a href='");
                buf.append(selectedColumn.getLocation(columnName));
                buf.append("#columns'><span class='notSortedByColumn'>");
                buf.append(columnName);
                buf.append("</span></a>");
            }
        } else {
            buf.append('>');
            buf.append(columnName);
        }
        buf.append("</th>");
        
        return buf.toString();
    }

    protected void writeFooter(LineWriter html) throws IOException {
        html.writeln("</table>");
        html.writeln("</div>");
        super.writeFooter(html);
    }

    protected boolean isColumnsPage() {
        return true;
    }
    
    private class ByColumnComparator implements Comparator {
        public int compare(Object object1, Object object2) {
            TableColumn column1 = (TableColumn)object1;
            TableColumn column2 = (TableColumn)object2;
            int rc = column1.getName().compareTo(column2.getName());
            if (rc == 0)
                rc = column1.getTable().getName().compareTo(column2.getTable().getName());
            return rc;
        }
    }
    
    private class ByTableComparator implements Comparator {
        public int compare(Object object1, Object object2) {
            TableColumn column1 = (TableColumn)object1;
            TableColumn column2 = (TableColumn)object2;
            int rc = column1.getTable().getName().compareTo(column2.getTable().getName());
            if (rc == 0)
                rc = column1.getName().compareTo(column2.getName());
            return rc;
        }
    }

    private class ByTypeComparator implements Comparator {
        private Comparator bySize = new BySizeComparator();
        
        public int compare(Object object1, Object object2) {
            TableColumn column1 = (TableColumn)object1;
            TableColumn column2 = (TableColumn)object2;
            int rc = column1.getType().compareTo(column2.getType());
            if (rc == 0) {
                rc = bySize.compare(column1, column2);
            }
            return rc;
        }
    }

    private class BySizeComparator implements Comparator {
        private Comparator byColumn = new ByColumnComparator();
        
        public int compare(Object object1, Object object2) {
            TableColumn column1 = (TableColumn)object1;
            TableColumn column2 = (TableColumn)object2;
            int rc = column1.getLength() - column2.getLength();
            if (rc == 0) {
                rc = column1.getDecimalDigits() - column2.getDecimalDigits();
                if (rc == 0)
                    rc = byColumn.compare(column1, column2);
            }
            return rc;
        }
    }

    private class ByNullableComparator implements Comparator {
        private Comparator byColumn = new ByColumnComparator();

        public int compare(Object object1, Object object2) {
            TableColumn column1 = (TableColumn)object1;
            TableColumn column2 = (TableColumn)object2;
            int rc = column1.isNullable() == column2.isNullable() ? 0 : column1.isNullable() ? -1 : 1;
            if (rc == 0)
                rc = byColumn.compare(column1, column2);
            return rc;
        }
    }

    private class ByAutoUpdateComparator implements Comparator {
        private Comparator byColumn = new ByColumnComparator();

        public int compare(Object object1, Object object2) {
            TableColumn column1 = (TableColumn)object1;
            TableColumn column2 = (TableColumn)object2;
            int rc = column1.isAutoUpdated() == column2.isAutoUpdated() ? 0 : column1.isAutoUpdated() ? -1 : 1;
            if (rc == 0)
                rc = byColumn.compare(column1, column2);
            return rc;
        }
    }

    private class ByDefaultValueComparator implements Comparator {
        private Comparator byColumn = new ByColumnComparator();

        public int compare(Object object1, Object object2) {
            TableColumn column1 = (TableColumn)object1;
            TableColumn column2 = (TableColumn)object2;
            int rc = String.valueOf(column1.getDefaultValue()).compareTo(String.valueOf(column2.getDefaultValue()));
            if (rc == 0)
                rc = byColumn.compare(column1, column2);
            return rc;
        }
    }

    private class ByChildrenComparator implements Comparator {
        private Comparator byColumn = new ByColumnComparator();

        public int compare(Object object1, Object object2) {
            TableColumn column1 = (TableColumn)object1;
            TableColumn column2 = (TableColumn)object2;
            Set childTables1 = new TreeSet();
            Set childTables2 = new TreeSet();
            
            Iterator iter = column1.getChildren().iterator();
            while (iter.hasNext()) {
                TableColumn column = (TableColumn)iter.next();
                if (!column.getParentConstraint(column1).isImplied())
                    childTables1.add(column.getTable().getName());
            }

            iter = column2.getChildren().iterator();
            while (iter.hasNext()) {
                TableColumn column = (TableColumn)iter.next();
                if (!column.getParentConstraint(column2).isImplied())
                    childTables2.add(column.getTable().getName());
            }
            
            int rc = childTables1.toString().compareTo(childTables2.toString());
            if (rc == 0)
                rc = byColumn.compare(column1, column2);
            return rc;
        }
    }

    private class ByParentsComparator implements Comparator {
        private Comparator byColumn = new ByColumnComparator();

        public int compare(Object object1, Object object2) {
            TableColumn column1 = (TableColumn)object1;
            TableColumn column2 = (TableColumn)object2;
            Set parentTables1 = new TreeSet();
            Set parentTables2 = new TreeSet();
            
            Iterator iter = column1.getParents().iterator();
            while (iter.hasNext()) {
                TableColumn column = (TableColumn)iter.next();
                if (!column.getChildConstraint(column1).isImplied())
                    parentTables1.add(column.getTable().getName());
            }
            
            iter = column2.getParents().iterator();
            while (iter.hasNext()) {
                TableColumn column = (TableColumn)iter.next();
                if (!column.getChildConstraint(column2).isImplied())
                    parentTables2.add(column.getTable().getName());
            }
            
            int rc = parentTables1.toString().compareTo(parentTables2.toString());
            if (rc == 0)
                rc = byColumn.compare(column1, column2);
            return rc;
        }
    }
}
