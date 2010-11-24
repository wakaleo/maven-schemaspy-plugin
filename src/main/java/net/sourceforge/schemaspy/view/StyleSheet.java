package net.sourceforge.schemaspy.view;

import java.io.*;
import java.util.*;
import net.sourceforge.schemaspy.util.*;

public class StyleSheet {
    private static StyleSheet instance;
    private String css;
    private String bodyBackgroundColor;
    private String tableHeadBackgroundColor;
    private String tableBackgroundColor;
    private String primaryKeyBackgroundColor;
    private String indexedColumnBackgroundColor;
    private String selectedTableBackgroundColor;
    private String excludedColumnBackgroundColor;
    private final List ids = new ArrayList();

    private StyleSheet(BufferedReader cssReader) throws IOException {
        String lineSeparator = System.getProperty("line.separator");
        StringBuffer data = new StringBuffer();
        String line;

        while ((line = cssReader.readLine()) != null) {
            data.append(line);
            data.append(lineSeparator);
        }

        css = data.toString();

        int startComment = data.indexOf("/*");
        while (startComment != -1) {
            int endComment = data.indexOf("*/");
            data.replace(startComment, endComment + 2, "");
            startComment = data.indexOf("/*");
        }

        StringTokenizer tokenizer = new StringTokenizer(data.toString(), "{}");
        String id = null;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            if (id == null) {
                id = token.toLowerCase();
                ids.add(id);
            } else {
                Map attribs = parseAttributes(token);
                if (id.equals(".content"))
                    bodyBackgroundColor = attribs.get("background").toString();
                else if (id.equals("th"))
                    tableHeadBackgroundColor = attribs.get("background-color").toString();
                else if (id.equals("td"))
                    tableBackgroundColor = attribs.get("background-color").toString();
                else if (id.equals(".primarykey"))
                    primaryKeyBackgroundColor = attribs.get("background").toString();
                else if (id.equals(".indexedcolumn"))
                    indexedColumnBackgroundColor = attribs.get("background").toString();
                else if (id.equals(".selectedtable"))
                    selectedTableBackgroundColor = attribs.get("background").toString();
                else if (id.equals(".excludedcolumn"))
                    excludedColumnBackgroundColor = attribs.get("background").toString();
                id = null;
            }
        }
    }

    public static StyleSheet getInstance() {
        return instance;
    }

    public static void init(BufferedReader cssReader) throws IOException {
        instance = new StyleSheet(cssReader);
    }

    private Map parseAttributes(String data) {
        Map attribs = new HashMap();

        try {
            StringTokenizer attrTokenizer = new StringTokenizer(data, ";");
            while (attrTokenizer.hasMoreTokens()) {
                StringTokenizer pairTokenizer = new StringTokenizer(attrTokenizer.nextToken(), ":");
                String attribute = pairTokenizer.nextToken().trim().toLowerCase();
                String value = pairTokenizer.nextToken().trim().toLowerCase();
                attribs.put(attribute, value);
            }
        } catch (NoSuchElementException badToken) {
            System.err.println("Failed to extract attributes from '" + data + "'");
            throw badToken;
        }

        return attribs;
    }

    public void write(LineWriter out) throws IOException {
        out.write(css);
    }

    public String getBodyBackground() {
        return bodyBackgroundColor;
    }

    public String getTableBackground() {
        return tableBackgroundColor;
    }

    public String getTableHeadBackground() {
        return tableHeadBackgroundColor;
    }

    public String getPrimaryKeyBackground() {
        return primaryKeyBackgroundColor;
    }

    public String getIndexedColumnBackground() {
        return indexedColumnBackgroundColor;
    }

    public String getSelectedTableBackground() {
        return selectedTableBackgroundColor;
    }

    public String getExcludedColumnBackgroundColor() {
        return excludedColumnBackgroundColor;
    }

    public int getOffsetOf(String id) {
        int offset = ids.indexOf(id.toLowerCase());
        if (offset == -1)
            throw new IllegalArgumentException(id);
        return offset;
    }
}
