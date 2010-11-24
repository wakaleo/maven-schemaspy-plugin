package net.sourceforge.schemaspy;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;
import net.sourceforge.schemaspy.util.*;
import net.sourceforge.schemaspy.view.*;

public class MultipleSchemaAnalyzer {
    private static MultipleSchemaAnalyzer instance = new MultipleSchemaAnalyzer();

    private MultipleSchemaAnalyzer() {
    }

    public static MultipleSchemaAnalyzer getInstance() {
        return instance;
    }

    public void analyze(String dbName, DatabaseMetaData meta, String schemaSpec, List args, String user, File outputDir, String loadedFrom) throws SQLException, IOException {
        long start = System.currentTimeMillis();
        List genericCommand = new ArrayList();
        genericCommand.add("java");
        genericCommand.add("-Doneofmultipleschemas=true");
        if (new File(loadedFrom).isDirectory()) {
            genericCommand.add("-cp");
            genericCommand.add(loadedFrom);
            genericCommand.add(Main.class.getName());
        } else {
            genericCommand.add("-jar");
            genericCommand.add(loadedFrom);
        }
        genericCommand.addAll(args);

        System.out.println("Analyzing schemas that match regular expression '" + schemaSpec + "':");
        System.out.println("(use -schemaSpec on command line or in .properties to exclude other schemas)");
        List populatedSchemas = getPopulatedSchemas(meta, schemaSpec, user);
        for (Iterator iter = populatedSchemas.iterator(); iter.hasNext(); )
            System.out.print(" " + iter.next());
        System.out.println();

        writeIndexPage(dbName, populatedSchemas, meta, outputDir);

        for (Iterator iter = populatedSchemas.iterator(); iter.hasNext(); ) {
            String schema = iter.next().toString();
            List command = new ArrayList(genericCommand);
            command.add("-s");
            command.add(schema);
            command.add("-o");
            command.add(new File(outputDir, schema).toString());
            System.out.println("Analyzing " + schema);
            Process java = Runtime.getRuntime().exec((String[])command.toArray(new String[]{}));
            new ProcessOutputReader(java.getInputStream(), System.out).start();
            new ProcessOutputReader(java.getErrorStream(), System.err).start();

            try {
                int rc = java.waitFor();
                if (rc != 0) {
                    System.err.println("Failed to execute this process (rc " + rc + "):");
                    iter = command.iterator();
                    while (iter.hasNext())
                        System.err.print(" " + iter.next());
                    System.err.println();
                    throw new RuntimeException("Failed to execute this process (rc " + rc + "):");
                }
            } catch (InterruptedException exc) {
            }
        }

        long end = System.currentTimeMillis();
        System.out.println();
        System.out.println("Wrote relationship details of " + populatedSchemas.size() + " schema" + (populatedSchemas.size() == 1 ? "" : "s") + " in " + (end - start) / 1000 + " seconds.");
        System.out.println("Start with " + new File(outputDir, "index.html"));
    }

    private void writeIndexPage(String dbName, List populatedSchemas, DatabaseMetaData meta, File outputDir) throws IOException {
        if (populatedSchemas.size() > 0) {
            LineWriter index = new LineWriter(new FileOutputStream(new File(outputDir, "index.html")));
            HtmlMultipleSchemasIndexPage.getInstance().write(dbName, populatedSchemas, meta, index);
            index.close();
        }
    }

    private List getPopulatedSchemas(DatabaseMetaData meta, String schemaSpec, String user) throws SQLException {
        List populatedSchemas;

        if (meta.supportsSchemasInTableDefinitions()) {
            Pattern schemaRegex = Pattern.compile(schemaSpec);

            populatedSchemas = DBAnalyzer.getPopulatedSchemas(meta, schemaSpec);
            Iterator iter = populatedSchemas.iterator();
            while (iter.hasNext()) {
                String schema = iter.next().toString();
                if (!schemaRegex.matcher(schema).matches())
                    iter.remove(); // remove those that we're not supposed to analyze
            }
        } else {
            populatedSchemas = Arrays.asList(new String[] {user});
        }

        return populatedSchemas;
    }

    private static class ProcessOutputReader extends Thread {
        private final Reader processReader;
        private final PrintStream out;

        ProcessOutputReader(InputStream processStream, PrintStream out) {
            processReader = new InputStreamReader(processStream);
            this.out = out;
            setDaemon(true);
        }

        public void run() {
            try {
                int ch;
                while ((ch = processReader.read()) != -1) {
                    out.print((char)ch);
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } finally {
                try {
                    processReader.close();
                } catch (Exception exc) {
                    exc.printStackTrace(); // shouldn't ever get here...but...
                }
            }
        }
    }
}
