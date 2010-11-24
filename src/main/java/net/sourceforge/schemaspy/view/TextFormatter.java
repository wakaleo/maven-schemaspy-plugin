package net.sourceforge.schemaspy.view;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.util.LineWriter;

public class TextFormatter {
    private static TextFormatter instance = new TextFormatter();

    /**
     * Singleton - prevent creation
     */
    private TextFormatter() {
    }

    public static TextFormatter getInstance() {
        return instance;
    }

    public void write(Collection tables, boolean includeViews, LineWriter out) throws IOException {
        for (Iterator iter = tables.iterator(); iter.hasNext(); ) {
            Table table = (Table)iter.next();
            if (!table.isView() || includeViews)
                out.writeln(table.getName());
        }
    }
}
