package net.sourceforge.schemaspy.util;

import java.io.*;

/**
 * BufferedWriter that adds a <code>writeln()</code> method
 * to output a <i>lineDelimited</i> line of text without
 * cluttering up code.
 */
public class LineWriter extends BufferedWriter {
    public LineWriter(Writer out) {
        super(out);
    }

    public LineWriter(Writer out, int sz) {
        super(out, sz);
    }

    /**
     * Construct a <code>LineWriter</code> with UTF8 output
     * @param out OutputStream
     * @throws UnsupportedEncodingException
     */
    public LineWriter(OutputStream out) throws UnsupportedEncodingException {
        super(new OutputStreamWriter(out, "UTF8"));
    }

    public void writeln(String str) throws IOException {
        write(str);
        newLine();
    }

    public void writeln() throws IOException {
        newLine();
    }
}
