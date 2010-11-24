package net.sourceforge.schemaspy.view;

import net.sourceforge.schemaspy.util.*;

public class HtmlGraphFormatter extends HtmlFormatter {
    private static boolean printedNoDotWarning = false;
    private static boolean printedInvalidVersionWarning = false;

    protected HtmlGraphFormatter() {
    }

    protected Dot getDot() {
        Dot dot = Dot.getInstance();
        if (!dot.exists()) {
            if (!printedNoDotWarning) {
                printedNoDotWarning = true;
                System.err.println();
                System.err.println("Warning: Failed to run dot.");
                System.err.println("   Download " + dot.getSupportedVersions());
                System.err.println("   from www.graphviz.org and make sure that dot is in your path.");
                System.err.println("   Generated pages will not contain a graphical view of table relationships.");
            }

            return null;
        }

        if (!dot.isValid()) {
            if (!printedInvalidVersionWarning) {
                printedInvalidVersionWarning = true;
                System.err.println();
                System.err.println("Warning: Invalid version of Graphviz dot detected (" + dot.getVersion() + ").");
                System.err.println("   SchemaSpy requires " + dot.getSupportedVersions() + ". from www.graphviz.org.");
                System.err.println("   Generated pages will not contain a graphical view of table relationships.");
            }

            return null;
        }

        return dot;
    }
}
