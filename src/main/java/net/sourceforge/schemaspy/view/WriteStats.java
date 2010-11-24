package net.sourceforge.schemaspy.view;

import java.util.*;
import java.util.regex.*;
import net.sourceforge.schemaspy.model.*;

/**
 * Simple ugly hack that provides details of what was written.
 */
public class WriteStats {
    private int numTables;
    private int numViews;
    private boolean includeImplied;
    private boolean wroteImplied;
    private boolean wroteTwoDegrees;
    private Pattern exclusionPattern;
    private final Set excludedColumns = new HashSet();

    public WriteStats(Pattern exclusionPattern, boolean includeImplied) {
        this.exclusionPattern = exclusionPattern;
        this.includeImplied = includeImplied;
    }

    public WriteStats(WriteStats stats) {
        this.exclusionPattern = stats.exclusionPattern;
        this.includeImplied = stats.includeImplied;
    }

    public void wroteTable(Table table) {
        if (table.isView())
            ++numViews;
        else
            ++numTables;
    }

    public void setWroteImplied(boolean wroteImplied) {
        this.wroteImplied = wroteImplied;
    }

    public void setWroteTwoDegrees(boolean wroteFocused) {
        this.wroteTwoDegrees = wroteFocused;
    }

    public int getNumTablesWritten() {
        return numTables;
    }

    public int getNumViewsWritten() {
        return numViews;
    }

    public boolean includeImplied() {
        return includeImplied;
    }

    public boolean wroteImplied() {
        return wroteImplied;
    }

    public boolean wroteTwoDegrees() {
        return wroteTwoDegrees;
    }

    public void addExcludedColumn(TableColumn column) {
        excludedColumns.add(column);
    }

    public Set getExcludedColumns() {
        return excludedColumns;
    }

    public Pattern getExclusionPattern() {
        return exclusionPattern;
    }

    /**
     * setIncludeImplied
     *
     * @param includeImplied boolean
     */
    public boolean setIncludeImplied(boolean includeImplied) {
        boolean oldValue = this.includeImplied;
        this.includeImplied = includeImplied;
        return oldValue;
    }

    /**
     * setExclusionPattern
     *
     * @param exclusionPattern Pattern
     * @return Pattern
     */
    public Pattern setExclusionPattern(Pattern exclusionPattern) {
        Pattern orig = this.exclusionPattern;
        this.exclusionPattern = exclusionPattern;
        return orig;
    }
}
