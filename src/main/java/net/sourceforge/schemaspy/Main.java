package net.sourceforge.schemaspy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.util.ConnectionURLBuilder;
import net.sourceforge.schemaspy.util.DOMUtil;
import net.sourceforge.schemaspy.util.Dot;
import net.sourceforge.schemaspy.util.LineWriter;
import net.sourceforge.schemaspy.view.DotFormatter;
import net.sourceforge.schemaspy.view.HtmlAnomaliesPage;
import net.sourceforge.schemaspy.view.HtmlColumnsPage;
import net.sourceforge.schemaspy.view.HtmlConstraintsPage;
import net.sourceforge.schemaspy.view.HtmlMainIndexPage;
import net.sourceforge.schemaspy.view.HtmlOrphansPage;
import net.sourceforge.schemaspy.view.HtmlRelationshipsPage;
import net.sourceforge.schemaspy.view.HtmlTablePage;
import net.sourceforge.schemaspy.view.ImageWriter;
import net.sourceforge.schemaspy.view.JavaScriptFormatter;
import net.sourceforge.schemaspy.view.StyleSheet;
import net.sourceforge.schemaspy.view.TextFormatter;
import net.sourceforge.schemaspy.view.WriteStats;
import net.sourceforge.schemaspy.view.XmlTableFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Main {

    public static void main(String[] argv) {
        try {
            List args = new ArrayList(Arrays.asList(argv)); // can't mod the
            // original

            if (args.size() == 0 || args.remove("-h") || args.remove("-?") || args.remove("?") || args.remove("/?")) {
                dumpUsage(null, false, false);
                System.exit(1);
            }

            if (args.remove("-help")) {
                dumpUsage(null, true, false);
                System.exit(1);
            }

            if (args.remove("-dbhelp")) {
                dumpUsage(null, true, true);
                System.exit(1);
            }

            long start = System.currentTimeMillis();
            long startGraphingDetails = start;
            long startSummarizing = start;

            // allow '=' in param specs
            args = fixupArgs(args);

            final boolean generateHtml = !args.remove("-nohtml");
            final boolean includeImpliedConstraints = !args.remove("-noimplied");

            String outputDirName = getRequiredParam(args, "-o");
            // quoting command-line arguments sometimes leaves the trailing "
            if (outputDirName.endsWith("\"")) {
                outputDirName = outputDirName.substring(0, outputDirName.length() - 1);
            }
            File outputDir = new File(outputDirName).getCanonicalFile();
            if (!outputDir.isDirectory()) {
                if (!outputDir.mkdirs()) {
                    System.err.println("Failed to create directory '" + outputDir + "'");
                    System.exit(2);
                }
            }

            String dbType = getParam(args, "-t");
            if (dbType == null) {
                dbType = "ora";
            }
            StringBuffer propertiesLoadedFrom = new StringBuffer();
            Properties properties = getDbProperties(dbType,
                    propertiesLoadedFrom);

            String user = getParam(args, "-u");
            String password = getParam(args, "-p");
            String schema = null;
            try {
                schema = getParam(args, "-s", false, true);
            } catch (Exception schemaNotSpecified) {
            }

            int maxDetailedTables = 300;
            try {
                maxDetailedTables = Integer.parseInt(getParam(args, "-maxdet"));
            } catch (Exception notSpecified) {
            }

            Properties userProperties = new Properties();
            String loc = getParam(args, "-connprops");
            if (loc != null) {
                userProperties.load(new FileInputStream(loc));
            }
            String classpath = getParam(args, "-cp");

            String css = getParam(args, "-css");
            if (css == null) {
                css = "schemaSpy.css";
            }
            String description = getParam(args, "-desc");

            int maxDbThreads = getMaxDbThreads(args, properties);

            // nasty hack, but passing this info everywhere churns my stomach
            System.setProperty("sourceforgelogo", String.valueOf(!args.remove("-nologo")));

            // and another nasty hack with the same justification as the one
            // above
            System.setProperty("rankdirbug", String.valueOf(args.remove("-rankdirbug")));

            // and yet another one (Allow Html In Comments - encode them unless
            // otherwise specified)
            System.setProperty("encodeComments", String.valueOf(!args.remove("-ahic")));

            // ugh, some more...
            System.setProperty("commentsInitiallyDisplayed", String.valueOf(args.remove("-cid")));
            System.setProperty("displayTableComments", String.valueOf(!args.remove("-notablecomments")));

            Pattern exclusions;
            String exclude = getParam(args, "-x");
            if (exclude != null) {
                exclusions = Pattern.compile(exclude);
            } else {
                exclusions = Pattern.compile("[^.]"); // match nothing

            }

            Pattern inclusions;
            String include = getParam(args, "-i");
            if (include != null) {
                inclusions = Pattern.compile(include);
            } else {
                inclusions = Pattern.compile(".*"); // match anything

            }

            String jdbcUrl = getParam(args, "-jdbcUrl");

            ConnectionURLBuilder urlBuilder = null;
            String connectionURL = jdbcUrl;
            try {
                urlBuilder = new ConnectionURLBuilder(dbType, args, properties);
                if (connectionURL == null) {
                    connectionURL = urlBuilder.getConnectionURL();
                }
            } catch (IllegalArgumentException badParam) {
                System.err.println(badParam.getMessage());
                System.exit(1);
            }

            String dbName = urlBuilder.getDbName();

            boolean analyzeAll = args.remove("-all");
            String schemaSpec = getParam(args, "-schemaSpec");


            String driverClass = properties.getProperty("driver");
            String driverPath = properties.getProperty("driverPath");

            boolean useDriverManager = false;

            String useDriverManagerValue = getParam(args, "-useDriverManager");
            if (useDriverManagerValue == null) {
                useDriverManagerValue = properties.getProperty("useDriverManager");
            }

            if (useDriverManagerValue != null) {
                useDriverManager = Boolean.valueOf(useDriverManagerValue);
            }
            boolean useCurrentClasspath = false;

            String useCurrentClasspathValue = getParam(args, "-useCurrentClasspath");
            if (useCurrentClasspathValue != null) {
                useCurrentClasspath = Boolean.valueOf(useCurrentClasspathValue);
            }

            if (args.size() != 0) {
                System.out.print("Warning: Unrecognized option(s):");
                for (Iterator iter = args.iterator(); iter.hasNext();) {
                    System.out.print(" " + iter.next());
                }
                System.out.println();
            }


            if (classpath != null) {
                driverPath = classpath + File.pathSeparator + driverPath;
            }
            Connection connection = getConnection(user, password, connectionURL,
                    driverClass, driverPath,
                    propertiesLoadedFrom.toString(), userProperties,
                    useDriverManager, useCurrentClasspath);
            DatabaseMetaData meta = connection.getMetaData();

            if (analyzeAll) {
                args = new ArrayList(Arrays.asList(argv));
                getParam(args, "-o"); // param will be replaced by something
                // appropriate

                getParam(args, "-s"); // param will be replaced by something
                // appropriate

                args.remove("-all"); // param will be replaced by something
                // appropriate

                if (schemaSpec == null) {
                    schemaSpec = properties.getProperty("schemaSpec", ".*");
                }
                MultipleSchemaAnalyzer.getInstance().analyze(dbName, meta,
                        schemaSpec, args, user, outputDir, getLoadedFromJar());
                System.exit(0);
            }

            if (schema == null && meta.supportsSchemasInTableDefinitions()) {
                schema = user;
            }

            if (generateHtml) {
                new File(outputDir, "tables").mkdirs();
                new File(outputDir, "graphs/summary").mkdirs();
                StyleSheet.init(new BufferedReader(getStyleSheet(css)));

                System.out.println("Connected to " + meta.getDatabaseProductName() + " - " + meta.getDatabaseProductVersion());
                System.out.println();
                System.out.print("Gathering schema details");
            }

            //
            // create the spy
            //
            SchemaSpy spy = new SchemaSpy(connection, meta, dbName, schema,
                    description, properties, inclusions, maxDbThreads);
            Database db = spy.getDatabase();

            LineWriter out;
            Collection tables = new ArrayList(db.getTables());
            tables.addAll(db.getViews());

            if (tables.isEmpty()) {
                dumpNoTablesMessage(schema, user, meta, include != null);
                System.exit(2);
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element rootNode = document.createElement("database");
            document.appendChild(rootNode);
            DOMUtil.appendAttribute(rootNode, "name", dbName);
            if (schema != null) {
                DOMUtil.appendAttribute(rootNode, "schema", schema);
            }
            DOMUtil.appendAttribute(rootNode, "type", db.getDatabaseProduct());

            if (generateHtml) {
                startSummarizing = System.currentTimeMillis();
                System.out.println("(" + (startSummarizing - start) / 1000 + "sec)");
                System.out.print("Writing/graphing summary");
                System.out.print(".");
                ImageWriter.getInstance().writeImages(outputDir);
                System.out.print(".");

                boolean showDetailedTables = tables.size() <= maxDetailedTables;

                File graphsDir = new File(outputDir, "graphs/summary");
                String dotBaseFilespec = "relationships";
                out = new LineWriter(new FileOutputStream(new File(graphsDir,
                        dotBaseFilespec + ".real.compact.dot")));
                WriteStats stats = new WriteStats(exclusions,
                        includeImpliedConstraints);
                DotFormatter.getInstance().writeRealRelationships(tables, true,
                        showDetailedTables, stats, out);
                boolean hasRealRelationships = stats.getNumTablesWritten() > 0 || stats.getNumViewsWritten() > 0;
                stats = new WriteStats(stats);
                out.close();

                if (hasRealRelationships) {
                    System.out.print(".");
                    out = new LineWriter(new FileOutputStream(new File(
                            graphsDir, dotBaseFilespec + ".real.large.dot")));
                    DotFormatter.getInstance().writeRealRelationships(tables,
                            false, showDetailedTables, stats, out);
                    stats = new WriteStats(stats);
                    out.close();
                }

                // getting implied constraints has a side-effect of associating
                // the parent/child tables, so don't do it
                // here unless they want that behavior
                List impliedConstraints = null;
                if (includeImpliedConstraints) {
                    impliedConstraints = DBAnalyzer.getImpliedConstraints(tables);
                } else {
                    impliedConstraints = new ArrayList();
                }
                List orphans = DBAnalyzer.getOrphans(tables);
                boolean hasOrphans = !orphans.isEmpty() && Dot.getInstance().isValid();

                System.out.print(".");

                File impliedDotFile = new File(graphsDir, dotBaseFilespec + ".implied.compact.dot");
                out = new LineWriter(new FileOutputStream(impliedDotFile));
                stats = new WriteStats(exclusions, includeImpliedConstraints);
                DotFormatter.getInstance().writeAllRelationships(tables, true,
                        showDetailedTables, stats, out);
                boolean hasImplied = stats.wroteImplied();
                Set excludedColumns = stats.getExcludedColumns();
                stats = new WriteStats(stats);
                out.close();
                if (hasImplied) {
                    impliedDotFile = new File(graphsDir, dotBaseFilespec + ".implied.large.dot");
                    out = new LineWriter(new FileOutputStream(impliedDotFile));
                    DotFormatter.getInstance().writeAllRelationships(tables,
                            false, showDetailedTables, stats, out);
                    stats = new WriteStats(stats);
                    out.close();
                } else {
                    impliedDotFile.delete();
                }

                out = new LineWriter(new FileWriter(new File(outputDir,
                        dotBaseFilespec + ".html")));
                HtmlRelationshipsPage.getInstance().write(db, graphsDir,
                        dotBaseFilespec, hasOrphans, hasRealRelationships,
                        hasImplied, excludedColumns, out);
                out.close();

                System.out.print(".");
                dotBaseFilespec = "utilities";
                out = new LineWriter(new FileWriter(new File(outputDir,
                        dotBaseFilespec + ".html")));
                HtmlOrphansPage.getInstance().write(db, orphans, graphsDir, out);
                stats = new WriteStats(stats);
                out.close();

                System.out.print(".");
                out = new LineWriter(new FileWriter(new File(outputDir,
                        "index.html")), 64 * 1024);
                HtmlMainIndexPage.getInstance().write(db, tables, hasOrphans,
                        out);
                stats = new WriteStats(stats);
                out.close();

                System.out.print(".");
                List constraints = DBAnalyzer.getForeignKeyConstraints(tables);
                out = new LineWriter(new FileWriter(new File(outputDir,
                        "constraints.html")), 256 * 1024);
                HtmlConstraintsPage constraintIndexFormatter = HtmlConstraintsPage.getInstance();
                constraintIndexFormatter.write(db, constraints, tables,
                        hasOrphans, out);
                stats = new WriteStats(stats);
                out.close();

                System.out.print(".");
                out = new LineWriter(new FileWriter(new File(outputDir,
                        "anomalies.html")), 16 * 1024);
                HtmlAnomaliesPage.getInstance().write(db, tables,
                        impliedConstraints, hasOrphans, out);
                stats = new WriteStats(stats);
                out.close();

                System.out.print(".");
                Iterator iter = HtmlColumnsPage.getInstance().getColumnInfos().iterator();
                while (iter.hasNext()) {
                    HtmlColumnsPage.ColumnInfo columnInfo = (HtmlColumnsPage.ColumnInfo) iter.next();
                    out = new LineWriter(new FileWriter(new File(outputDir,
                            columnInfo.getLocation())), 16 * 1024);
                    HtmlColumnsPage.getInstance().write(db, tables, columnInfo,
                            hasOrphans, out);
                    stats = new WriteStats(stats);
                    out.close();
                }

                startGraphingDetails = System.currentTimeMillis();
                System.out.println("(" + (startGraphingDetails - startSummarizing) / 1000 + "sec)");
                System.out.print("Writing/graphing results");

                HtmlTablePage tableFormatter = HtmlTablePage.getInstance();
                for (iter = tables.iterator(); iter.hasNext();) {
                    System.out.print('.');
                    Table table = (Table) iter.next();
                    out = new LineWriter(new FileWriter(new File(outputDir,
                            "tables/" + table.getName() + ".html")), 24 * 1024);
                    tableFormatter.write(db, table, hasOrphans, outputDir,
                            stats, out);
                    stats = new WriteStats(stats);
                    out.close();
                }

                out = new LineWriter(new FileWriter(new File(outputDir,
                        "schemaSpy.css")));
                StyleSheet.getInstance().write(out);
                out.close();
                out = new LineWriter(new FileWriter(new File(outputDir,
                        "schemaSpy.js")));
                JavaScriptFormatter.getInstance().write(out);
                out.close();
            }

            XmlTableFormatter.getInstance().appendTables(rootNode, tables);

            String xmlName = dbName;
            if (schema != null) {
                xmlName += '.' + schema;
            // use OutputStream constructor to force it to use UTF8 (per Bernard
            // D'Hav�)
            }
            out = new LineWriter(new FileOutputStream(new File(outputDir,
                    xmlName + ".xml")));
            document.getDocumentElement().normalize();
            DOMUtil.printDOM(document, out);
            out.close();

            // 'try' to make some memory available for the sorting process
            // (some people have run out of memory while RI sorting tables)
            builder = null;
            connection = null;
            db = null;
            document = null;
            factory = null;
            meta = null;
            properties = null;
            rootNode = null;
            urlBuilder = null;

            List recursiveConstraints = new ArrayList();

            // side effect is that the RI relationships get trashed
            // also populates the recursiveConstraints collection
            List orderedTables = spy.sortTablesByRI(recursiveConstraints);

            out = new LineWriter(new FileWriter(new File(outputDir,
                    "insertionOrder.txt")), 16 * 1024);
            TextFormatter.getInstance().write(orderedTables, false, out);
            out.close();

            out = new LineWriter(new FileWriter(new File(outputDir,
                    "deletionOrder.txt")), 16 * 1024);
            Collections.reverse(orderedTables);
            TextFormatter.getInstance().write(orderedTables, false, out);
            out.close();

            /*
             * we'll eventually want to put this functionality back in with a
             * database independent implementation File constraintsFile = new
             * File(outputDir, "removeRecursiveConstraints.sql");
             * constraintsFile.delete(); if (!recursiveConstraints.isEmpty()) {
             * out = new LineWriter(new FileWriter(constraintsFile), 4 * 1024);
             * writeRemoveRecursiveConstraintsSql(recursiveConstraints, schema,
             * out); out.close(); }
             *
             * constraintsFile = new File(outputDir,
             * "restoreRecursiveConstraints.sql"); constraintsFile.delete();
             *
             * if (!recursiveConstraints.isEmpty()) { out = new LineWriter(new
             * FileWriter(constraintsFile), 4 * 1024);
             * writeRestoreRecursiveConstraintsSql(recursiveConstraints, schema,
             * out); out.close(); }
             */

            if (generateHtml) {
                long end = System.currentTimeMillis();
                System.out.println("(" + (end - startGraphingDetails) / 1000 + "sec)");
                System.out.println("Wrote relationship details of " + tables.size() + " tables/views to directory '" + new File(outputDirName) + "' in " + (end - start) / 1000 + " seconds.");
                System.out.println("Start with " + new File(outputDirName, "index.html"));
            }
        } catch (Exception exc) {
            System.err.println();
            exc.printStackTrace();
        }
    }

    /**
     * getMaxDbThreads
     *
     * @param args
     *            List
     * @param properties
     *            Properties
     * @return int
     */
    private static int getMaxDbThreads(List args, Properties properties) {
        int maxThreads = Integer.MAX_VALUE;
        String threads = properties.getProperty("dbThreads");
        if (threads == null) {
            threads = properties.getProperty("dbthreads");
        }
        if (threads != null) {
            maxThreads = Integer.parseInt(threads);
        }
        threads = getParam(args, "-dbThreads");
        if (threads == null) {
            threads = getParam(args, "-dbthreads");
        }
        if (threads != null) {
            maxThreads = Integer.parseInt(threads);
        }
        if (maxThreads < 0) {
            maxThreads = Integer.MAX_VALUE;
        } else if (maxThreads == 0) {
            maxThreads = 1;
        }
        return maxThreads;
    }

    /**
     * dumpNoDataMessage
     *
     * @param schema
     *            String
     * @param user
     *            String
     * @param meta
     *            DatabaseMetaData
     */
    private static void dumpNoTablesMessage(String schema, String user,
            DatabaseMetaData meta, boolean specifiedInclusions)
            throws SQLException {
        System.out.println();
        System.out.println();
        System.out.println("No tables or views were found in schema '" + schema + "'.");
        List schemas = DBAnalyzer.getSchemas(meta);
        if (schema == null || schemas.contains(schema)) {
            System.out.println("The schema exists in the database, but the user you specified (" + user + ')');
            System.out.println("  might not have rights to read its contents.");
            if (specifiedInclusions) {
                System.out.println("Another possibility is that the regular expression that you specified");
                System.out.println("  for what to include (via -i) didn't match any tables.");
            }
        } else {
            System.out.println("The schema does not exist in the database.");
            System.out.println("Make sure that you specify a valid schema with the -s option and that");
            System.out.println("  the user specified (" + user + ") can read from the schema.");
            System.out.println("Note that schema names are usually case sensitive.");
        }
        System.out.println();
        boolean plural = schemas.size() != 1;
        System.out.println(schemas.size() + " schema" + (plural ? "s" : "") + " exist" + (plural ? "" : "s") + " in this database.");
        System.out.println("Some of these \"schemas\" may be users or system schemas.");
        System.out.println();
        Iterator iter = schemas.iterator();
        while (iter.hasNext()) {
            System.out.print(iter.next() + " ");
        }

        System.out.println();
        System.out.println("These schemas contain tables/views that user '" + user + "' can see:");
        System.out.println();
        iter = DBAnalyzer.getPopulatedSchemas(meta).iterator();
        while (iter.hasNext()) {
            System.out.print(iter.next() + " ");
        }
    }

    private static Connection getConnection(String user, String password,
            String connectionURL, String driverClass, String driverPath,
            String propertiesLoadedFrom, Properties userProperties,
            boolean useDriverManager, boolean useCurrentClasspath)
            throws MalformedURLException {
        System.out.println("Using database properties:");
        System.out.println("    " + propertiesLoadedFrom);

        List classpath = new ArrayList();
        List invalidClasspathEntries = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(driverPath,
                File.pathSeparator);
        while (tokenizer.hasMoreTokens()) {
            File pathElement = new File(tokenizer.nextToken());
            if (pathElement.exists()) {
                classpath.add(pathElement.toURL());
            } else {
                invalidClasspathEntries.add(pathElement);
            }
        }

        URLClassLoader loader = new URLClassLoader((URL[]) classpath.toArray(new URL[0]));
        Driver driver = null;

        try {
            if (useCurrentClasspath) {
                try {
                    driver = (Driver) Class.forName(driverClass).newInstance();
                } catch (Exception e) {
                    System.err.println("Failed to load JDBC driver:");
                    e.printStackTrace();
                }
            } else {
                Class.forName(driverClass);
                driver = (Driver) Class.forName(driverClass, true, loader).newInstance();

            // have to use deprecated method or we won't see messages generated
            // by older drivers
            // java.sql.DriverManager.setLogStream(System.err);
            }
        } catch (Exception exc) {
            System.err.println(exc); // people don't want to see a stack
            // trace...

            System.err.println();
            System.err.println("Failed to load driver '" + driverClass + "' from: " + classpath);
            if (!invalidClasspathEntries.isEmpty()) {
                if (invalidClasspathEntries.size() == 1) {
                    System.err.print("This entry doesn't point to a valid file/directory: ");
                } else {
                    System.err.print("These entries don't point to valid files/directories: ");
                }
                System.err.println(invalidClasspathEntries);
            }
            System.err.println();
            System.err.println("Use -t [databaseType] to specify what drivers to use or modify");
            System.err.println("one of the .properties from the jar, put it on your file");
            System.err.println("system and point to it with -t [databasePropertiesFile].");
            System.err.println();
            System.err.println("For many people it's easiest to use the -cp option to directly specify");
            System.err.println("where the database drivers exist (usually in a .jar or .zip/.Z).");
            System.err.println("Note that the -cp option must be specified after " + getLoadedFromJar());
            System.err.println();
            System.exit(1);
        }

        Properties connectionProperties = new Properties();
        if (user != null) {
            connectionProperties.put("user", user);
        }
        if (password != null) {
            connectionProperties.put("password", password);
        }
        connectionProperties.putAll(userProperties);

        Connection connection = null;
        try {
            if (useDriverManager) {
                connection = java.sql.DriverManager.getConnection(connectionURL, connectionProperties);

            } else {
                connection = driver.connect(connectionURL, connectionProperties);
            }
            /*
             * if (connectionProperties.size() > 0) { connection =
             * driver.connect(connectionURL, connectionProperties); } else {
             * connection = java.sql.DriverManager.getConnection(connectionURL); }
             */
            if (connection == null) {
                System.err.println();
                System.err.println("Cannot connect to this database URL:");
                System.err.println("  " + connectionURL);
                System.err.println("with this driver:");
                System.err.println("  " + driverClass);
                System.err.println();
                System.err.println("Additional connection information may be available in ");
                System.err.println("  " + propertiesLoadedFrom);
                System.exit(1);
            }
        } catch (UnsatisfiedLinkError badPath) {
            System.err.println();
            System.err.println("Failed to load driver [" + driverClass + "] from classpath " + classpath);
            System.err.println();
            System.err.println("Make sure the reported library (.dll/.lib/.so) from the following line can be");
            System.err.println("found by your PATH (or LIB*PATH) environment variable");
            System.err.println();
            badPath.printStackTrace();
            System.exit(1);
        } catch (Exception exc) {
            System.err.println();
            System.err.println("Failed to connect to database URL [" + connectionURL + "]");
            System.err.println();
            exc.printStackTrace();
            System.exit(1);
        }

        return connection;
    }

    /**
     * Currently very DB2-specific
     *
     * @param recursiveConstraints
     *            List
     * @param schema
     *            String
     * @param out
     *            LineWriter
     * @throws IOException
     */
    /*
     * we'll eventually want to put this functionality back in with a database
     * independent implementation private static void
     * writeRemoveRecursiveConstraintsSql(List recursiveConstraints, String
     * schema, LineWriter out) throws IOException { for (Iterator iter =
     * recursiveConstraints.iterator(); iter.hasNext(); ) { ForeignKeyConstraint
     * constraint = (ForeignKeyConstraint)iter.next(); out.writeln("ALTER TABLE " +
     * schema + "." + constraint.getChildTable() + " DROP CONSTRAINT " +
     * constraint.getName() + ";"); } }
     */
    /**
     * Currently very DB2-specific
     *
     * @param recursiveConstraints
     *            List
     * @param schema
     *            String
     * @param out
     *            LineWriter
     * @throws IOException
     */
    /*
     * we'll eventually want to put this functionality back in with a database
     * independent implementation private static void
     * writeRestoreRecursiveConstraintsSql(List recursiveConstraints, String
     * schema, LineWriter out) throws IOException { Map ruleTextMapping = new
     * HashMap(); ruleTextMapping.put(new Character('C'), "CASCADE");
     * ruleTextMapping.put(new Character('A'), "NO ACTION");
     * ruleTextMapping.put(new Character('N'), "NO ACTION"); // Oracle
     * ruleTextMapping.put(new Character('R'), "RESTRICT");
     * ruleTextMapping.put(new Character('S'), "SET NULL"); // Oracle
     *
     * for (Iterator iter = recursiveConstraints.iterator(); iter.hasNext(); ) {
     * ForeignKeyConstraint constraint = (ForeignKeyConstraint)iter.next();
     * out.write("ALTER TABLE \"" + schema + "\".\"" +
     * constraint.getChildTable() + "\" ADD CONSTRAINT \"" +
     * constraint.getName() + "\""); StringBuffer buf = new StringBuffer(); for
     * (Iterator columnIter = constraint.getChildColumns().iterator();
     * columnIter.hasNext(); ) { buf.append("\"");
     * buf.append(columnIter.next()); buf.append("\""); if
     * (columnIter.hasNext()) buf.append(","); } out.write(" FOREIGN KEY (" +
     * buf.toString() + ")"); out.write(" REFERENCES \"" + schema + "\".\"" +
     * constraint.getParentTable() + "\""); buf = new StringBuffer(); for
     * (Iterator columnIter = constraint.getParentColumns().iterator();
     * columnIter.hasNext(); ) { buf.append("\"");
     * buf.append(columnIter.next()); buf.append("\""); if
     * (columnIter.hasNext()) buf.append(","); } out.write(" (" + buf.toString() +
     * ")"); out.write(" ON DELETE "); out.write(ruleTextMapping.get(new
     * Character(constraint.getDeleteRule())).toString()); out.write(" ON UPDATE
     * "); out.write(ruleTextMapping.get(new
     * Character(constraint.getUpdateRule())).toString()); out.writeln(";"); } }
     */
    private static void dumpUsage(String errorMessage, boolean detailed,
            boolean detailedDb) {
        if (errorMessage != null) {
            System.err.println("*** " + errorMessage + " ***");
        }

        if (detailed) {
            System.out.println("SchemaSpy generates an HTML representation of a database's relationships.");
            System.out.println();
        }

        if (!detailedDb) {
            System.out.println("Usage:");
            System.out.println(" java -jar " + getLoadedFromJar() + " [options]");
            System.out.println("   -t databaseType       type of database - defaults to ora");
            System.out.println("                           use -dbhelp for a list of built-in types");
            System.out.println("   -u user               connect to the database with this user id");
            System.out.println("   -s schema             defaults to the specified user");
            System.out.println("   -p password           defaults to no password");
            System.out.println("   -o outputDirectory    directory to place the generated output in");
            System.out.println("   -cp pathToDrivers     optional - looks for drivers here before looking");
            System.out.println("                           in driverPath in [databaseType].properties.");
            System.out.println("                           must be specified after " + getLoadedFromJar());
            System.out.println("Go to http://schemaspy.sourceforge.net for a complete list/description");
            System.out.println(" of additional parameters.");
            System.out.println();
        }

        if (!detailed) {
            System.out.println(" java -jar " + getLoadedFromJar() + " -help to display more detailed help");
            System.out.println();
        }

        if (detailedDb) {
            System.out.println("Built-in database types and their required connection parameters:");
            Set datatypes = getBuiltInDatabaseTypes(getLoadedFromJar());
            class DbPropLoader {

                Properties load(String dbType) {
                    ResourceBundle bundle = ResourceBundle.getBundle(dbType);
                    Properties properties;
                    try {
                        String baseDbType = bundle.getString("extends");
                        int lastSlash = dbType.lastIndexOf('/');
                        if (lastSlash != -1) {
                            baseDbType = dbType.substring(0, dbType.lastIndexOf("/") + 1) + baseDbType;
                        }
                        properties = load(baseDbType);
                    } catch (MissingResourceException doesntExtend) {
                        properties = new Properties();
                    }

                    return add(properties, bundle);
                }
            }

            for (Iterator iter = datatypes.iterator(); iter.hasNext();) {
                String dbType = iter.next().toString();
                new ConnectionURLBuilder(dbType, null, new DbPropLoader().load(dbType)).dumpUsage();
            }
            System.out.println();
        }

        if (detailed || detailedDb) {
            System.out.println("You can use your own database types by specifying the filespec of a .properties file with -t.");
            System.out.println("Grab one out of " + getLoadedFromJar() + " and modify it to suit your needs.");
            System.out.println();
        }

        if (detailed) {
            System.out.println("Sample usage using the default database type (implied -t ora):");
            System.out.println(" java -jar schemaSpy.jar -db epdb -s sonedba -u devuser -p devuser -o output");
            System.out.println();
        }
    }

    public static String getLoadedFromJar() {
        String classpath = System.getProperty("java.class.path");
        return new StringTokenizer(classpath, File.pathSeparator).nextToken();
    }

    private static String getParam(List args, String paramId) {
        return getParam(args, paramId, false, false);
    }

    private static String getRequiredParam(List args, String paramId) {
        return getParam(args, paramId, true, false);
    }

    private static String getParam(List args, String paramId, boolean required,
            boolean dbTypeSpecific) {
        int paramIndex = args.indexOf(paramId);
        if (paramIndex < 0) {
            if (required) {
                dumpUsage(
                        "Parameter '" + paramId + "' missing." + (dbTypeSpecific ? "  It is required for this database type."
                        : ""), !dbTypeSpecific, dbTypeSpecific);
                System.exit(1);
            } else {
                return null;
            }
        }
        args.remove(paramIndex);
        String param = args.get(paramIndex).toString();
        args.remove(paramIndex);
        return param;
    }

    /**
     * Allow an equal sign in args...like "-db=dbName"
     *
     * @param args
     *            List
     * @return List
     */
    private static List fixupArgs(List args) {
        List expandedArgs = new ArrayList();

        Iterator iter = args.iterator();
        while (iter.hasNext()) {
            String arg = iter.next().toString();
            int indexOfEquals = arg.indexOf('=');
            if (indexOfEquals != -1 && arg.indexOf('\\') == indexOfEquals - 1) { // "\="
                // (escaped)

                expandedArgs.add(arg.substring(0, indexOfEquals - 1) + arg.substring(indexOfEquals));
            } else if (indexOfEquals != -1) {
                expandedArgs.add(arg.substring(0, indexOfEquals));
                expandedArgs.add(arg.substring(indexOfEquals + 1));
            } else {
                expandedArgs.add(arg);
            }
        }

        return expandedArgs;
    }

    private static Properties getDbProperties(String dbType,
            StringBuffer loadedFrom) throws IOException {
        ResourceBundle bundle = null;

        try {
            File propertiesFile = new File(dbType);
            bundle = new PropertyResourceBundle(new FileInputStream(
                    propertiesFile));
            loadedFrom.append(propertiesFile.getAbsolutePath());
        } catch (FileNotFoundException notFoundOnFilesystemWithoutExtension) {
            try {
                File propertiesFile = new File(dbType + ".properties");
                bundle = new PropertyResourceBundle(new FileInputStream(
                        propertiesFile));
                loadedFrom.append(propertiesFile.getAbsolutePath());
            } catch (FileNotFoundException notFoundOnFilesystemWithExtensionTackedOn) {
                try {
                    bundle = ResourceBundle.getBundle(dbType);
                    loadedFrom.append("[" + getLoadedFromJar() + "]" + File.separator + dbType + ".properties");
                } catch (Exception notInJarWithoutPath) {
                    try {
                        String path = SchemaSpy.class.getPackage().getName() + ".dbTypes." + dbType;
                        path = path.replace('.', '/');
                        bundle = ResourceBundle.getBundle(path);
                        loadedFrom.append("[" + getLoadedFromJar() + "]/" + path + ".properties");
                    } catch (Exception notInJar) {
                        notInJar.printStackTrace();
                        notFoundOnFilesystemWithExtensionTackedOn.printStackTrace();
                        throw notFoundOnFilesystemWithoutExtension;
                    }
                }
            }
        }

        Properties properties;

        try {
            String baseDbType = bundle.getString("extends").trim();
            properties = getDbProperties(baseDbType, new StringBuffer());
        } catch (MissingResourceException doesntExtend) {
            properties = new Properties();
        }

        return add(properties, bundle);
    }

    /**
     * Add the contents of <code>bundle</code> to the specified
     * <code>properties</code>.
     *
     * @param properties
     *            Properties
     * @param bundle
     *            ResourceBundle
     * @return Properties
     */
    private static Properties add(Properties properties, ResourceBundle bundle) {
        Enumeration iter = bundle.getKeys();
        while (iter.hasMoreElements()) {
            Object key = iter.nextElement();
            properties.put(key, bundle.getObject(key.toString()));
        }

        return properties;
    }

    public static Set getBuiltInDatabaseTypes(String loadedFromJar) {
        Set databaseTypes = new TreeSet();
        JarInputStream jar = null;

        try {
            jar = new JarInputStream(new FileInputStream(loadedFromJar));
            JarEntry entry;

            while ((entry = jar.getNextJarEntry()) != null) {
                String entryName = entry.getName();
                int dotPropsIndex = entryName.indexOf(".properties");
                if (dotPropsIndex != -1) {
                    databaseTypes.add(entryName.substring(0, dotPropsIndex));
                }
            }
        } catch (IOException exc) {
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException ignore) {
                }
            }
        }

        return databaseTypes;
    }

    private static Reader getStyleSheet(String cssName) throws IOException {
        File cssFile = new File(cssName);
        if (cssFile.exists()) {
            return new FileReader(cssFile);
        }
        cssFile = new File(System.getProperty("user.dir"), cssName);
        if (cssFile.exists()) {
            return new FileReader(cssFile);
        }
        InputStream cssStream = StyleSheet.class.getClassLoader().getResourceAsStream(cssName);
        if (cssStream == null) {
            throw new IllegalStateException(
                    "Unable to find requested style sheet: " + cssName);
        }
        return new InputStreamReader(cssStream);
    }
}
