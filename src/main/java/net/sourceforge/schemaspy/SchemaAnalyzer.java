/*
 * This file is a part of the SchemaSpy project (http://schemaspy.sourceforge.net).
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 John Currier
 *
 * SchemaSpy is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * SchemaSpy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.sourceforge.schemaspy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.sourceforge.schemaspy.model.ConnectionFailure;
import net.sourceforge.schemaspy.model.Database;
import net.sourceforge.schemaspy.model.EmptySchemaException;
import net.sourceforge.schemaspy.model.ForeignKeyConstraint;
import net.sourceforge.schemaspy.model.ImpliedForeignKeyConstraint;
import net.sourceforge.schemaspy.model.InvalidConfigurationException;
import net.sourceforge.schemaspy.model.Table;
import net.sourceforge.schemaspy.model.TableColumn;
import net.sourceforge.schemaspy.model.xml.SchemaMeta;
import net.sourceforge.schemaspy.util.ConnectionURLBuilder;
import net.sourceforge.schemaspy.util.DOMUtil;
import net.sourceforge.schemaspy.util.DbSpecificOption;
import net.sourceforge.schemaspy.util.Dot;
import net.sourceforge.schemaspy.util.LineWriter;
import net.sourceforge.schemaspy.util.LogFormatter;
import net.sourceforge.schemaspy.util.PasswordReader;
import net.sourceforge.schemaspy.util.ResourceWriter;
import net.sourceforge.schemaspy.view.DotFormatter;
import net.sourceforge.schemaspy.view.HtmlAnomaliesPage;
import net.sourceforge.schemaspy.view.HtmlColumnsPage;
import net.sourceforge.schemaspy.view.HtmlConstraintsPage;
import net.sourceforge.schemaspy.view.HtmlMainIndexPage;
import net.sourceforge.schemaspy.view.HtmlOrphansPage;
import net.sourceforge.schemaspy.view.HtmlRelationshipsPage;
import net.sourceforge.schemaspy.view.HtmlTablePage;
import net.sourceforge.schemaspy.view.ImageWriter;
import net.sourceforge.schemaspy.view.StyleSheet;
import net.sourceforge.schemaspy.view.TextFormatter;
import net.sourceforge.schemaspy.view.WriteStats;
import net.sourceforge.schemaspy.view.XmlTableFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author John Currier
 */
public class SchemaAnalyzer {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private boolean fineEnabled;

    public Database analyze(Config config) throws Exception {
        try {
            if (config.isHelpRequired()) {
                config.dumpUsage(null, false);
                return null;
            }

            if (config.isDbHelpRequired()) {
                config.dumpUsage(null, true);
                return null;
            }

            // set the log level for the root logger
            Logger.getLogger("").setLevel(config.getLogLevel());

            // clean-up console output a bit
            for (Handler handler : Logger.getLogger("").getHandlers()) {
                if (handler instanceof ConsoleHandler) {
                    ((ConsoleHandler)handler).setFormatter(new LogFormatter());
                    handler.setLevel(config.getLogLevel());
                }
            }

            fineEnabled = logger.isLoggable(Level.FINE);
            logger.info("Starting schema analysis");

            long start = System.currentTimeMillis();
            long startDiagrammingDetails = start;
            long startSummarizing = start;

            File outputDir = config.getOutputDir();
            if (!outputDir.isDirectory()) {
                if (!outputDir.mkdirs()) {
                    throw new IOException("Failed to create directory '" + outputDir + "'");
                }
            }

            List<String> schemas = config.getSchemas();
            if (schemas != null) {
                List<String> args = config.asList();

                // following params will be replaced by something appropriate
                yankParam(args, "-o");
                yankParam(args, "-s");
                args.remove("-all");
                args.remove("-schemas");
                args.remove("-schemata");

                String dbName = config.getDb();

                MultipleSchemaAnalyzer.getInstance().analyze(dbName, schemas, args, config.getUser(), outputDir, config.getCharset(), Config.getLoadedFromJar());
                return null;
            }

            Properties properties = config.getDbProperties(config.getDbType());

            ConnectionURLBuilder urlBuilder = new ConnectionURLBuilder(config, properties);
            if (config.getDb() == null)
                config.setDb(urlBuilder.getConnectionURL());

            if (config.getRemainingParameters().size() != 0) {
                StringBuilder msg = new StringBuilder("Unrecognized option(s):");
                for (String remnant : config.getRemainingParameters())
                    msg.append(" " + remnant);
                logger.warning(msg.toString());
            }

            String driverClass = properties.getProperty("driver");
            String driverPath = properties.getProperty("driverPath");
            if (driverPath == null)
                driverPath = "";
            if (config.getDriverPath() != null)
                driverPath = config.getDriverPath() + File.pathSeparator + driverPath;

            Connection connection = getConnection(config, urlBuilder.getConnectionURL(), driverClass, driverPath);

            DatabaseMetaData meta = connection.getMetaData();
            String dbName = config.getDb();
            String schema = config.getSchema();

            if (config.isEvaluateAllEnabled()) {
                List<String> args = config.asList();
                for (DbSpecificOption option : urlBuilder.getOptions()) {
                    if (!args.contains("-" + option.getName())) {
                        args.add("-" + option.getName());
                        args.add(option.getValue().toString());
                    }
                }

                yankParam(args, "-o");  // param will be replaced by something appropriate
                yankParam(args, "-s");  // param will be replaced by something appropriate
                args.remove("-all");    // param will be replaced by something appropriate

                String schemaSpec = config.getSchemaSpec();
                if (schemaSpec == null)
                    schemaSpec = properties.getProperty("schemaSpec", ".*");
                MultipleSchemaAnalyzer.getInstance().analyze(dbName, meta, schemaSpec, null, args, config.getUser(), outputDir, config.getCharset(), Config.getLoadedFromJar());
                return null;    // no database to return
            }

            if (schema == null && meta.supportsSchemasInTableDefinitions() &&
                    !config.isSchemaDisabled()) {
                schema = config.getUser();
                if (schema == null)
                    throw new InvalidConfigurationException("Either a schema ('-s') or a user ('-u') must be specified");
                config.setSchema(schema);
            }

            SchemaMeta schemaMeta = config.getMeta() == null ? null : new SchemaMeta(config.getMeta(), dbName, schema);
            if (config.isHtmlGenerationEnabled()) {
                new File(outputDir, "tables").mkdirs();
                new File(outputDir, "diagrams/summary").mkdirs();

                logger.info("Connected to " + meta.getDatabaseProductName() + " - " + meta.getDatabaseProductVersion());

                if (schemaMeta != null && schemaMeta.getFile() != null) {
                    logger.info("Using additional metadata from " + schemaMeta.getFile());
                }

                logger.info("Gathering schema details");

                if (!fineEnabled)
                    System.out.print("Gathering schema details...");
            }

            //
            // create our representation of the database
            //
            Database db = new Database(config, connection, meta, dbName, schema, properties, schemaMeta);

            schemaMeta = null; // done with it so let GC reclaim it

            LineWriter out;
            Collection<Table> tables = new ArrayList<Table>(db.getTables());
            tables.addAll(db.getViews());

            if (tables.isEmpty()) {
                dumpNoTablesMessage(schema, config.getUser(), meta, config.getTableInclusions() != null);
                if (!config.isOneOfMultipleSchemas()) // don't bail if we're doing the whole enchilada
                    throw new EmptySchemaException();
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element rootNode = document.createElement("database");
            document.appendChild(rootNode);
            DOMUtil.appendAttribute(rootNode, "name", dbName);
            if (schema != null)
                DOMUtil.appendAttribute(rootNode, "schema", schema);
            DOMUtil.appendAttribute(rootNode, "type", db.getDatabaseProduct());

            if (config.isHtmlGenerationEnabled()) {
                startSummarizing = System.currentTimeMillis();
                if (!fineEnabled) {
                    System.out.println("(" + (startSummarizing - start) / 1000 + "sec)");
                }

                logger.info("Gathered schema details in " + (startSummarizing - start) / 1000 + " seconds");
                logger.info("Writing/graphing summary");
                System.err.flush();
                System.out.flush();
                if (!fineEnabled) {
                    System.out.print("Writing/graphing summary");
                    System.out.print(".");
                }
                ImageWriter.getInstance().writeImages(outputDir);
                ResourceWriter.getInstance().writeResource("/jquery.js", new File(outputDir, "/jquery.js"));
                ResourceWriter.getInstance().writeResource("/schemaSpy.js", new File(outputDir, "/schemaSpy.js"));
                if (!fineEnabled)
                    System.out.print(".");

                boolean showDetailedTables = tables.size() <= config.getMaxDetailedTables();
                final boolean includeImpliedConstraints = config.isImpliedConstraintsEnabled();

                // if evaluating a 'ruby on rails-based' database then connect the columns
                // based on RoR conventions
                // note that this is done before 'hasRealRelationships' gets evaluated so
                // we get a relationships ER diagram
                if (config.isRailsEnabled())
                    DbAnalyzer.getRailsConstraints(db.getTablesByName());

                File diagramsDir = new File(outputDir, "diagrams/summary");

                // generate the compact form of the relationships .dot file
                String dotBaseFilespec = "relationships";
                out = new LineWriter(new File(diagramsDir, dotBaseFilespec + ".real.compact.dot"), Config.DOT_CHARSET);
                WriteStats stats = new WriteStats(tables);
                DotFormatter.getInstance().writeRealRelationships(db, tables, true, showDetailedTables, stats, out);
                boolean hasRealRelationships = stats.getNumTablesWritten() > 0 || stats.getNumViewsWritten() > 0;
                out.close();

                if (hasRealRelationships) {
                    // real relationships exist so generate the 'big' form of the relationships .dot file
                    if (!fineEnabled)
                        System.out.print(".");
                    out = new LineWriter(new File(diagramsDir, dotBaseFilespec + ".real.large.dot"), Config.DOT_CHARSET);
                    DotFormatter.getInstance().writeRealRelationships(db, tables, false, showDetailedTables, stats, out);
                    out.close();
                }

                // getting implied constraints has a side-effect of associating the parent/child tables, so don't do it
                // here unless they want that behavior
                List<ImpliedForeignKeyConstraint> impliedConstraints = null;
                if (includeImpliedConstraints)
                    impliedConstraints = DbAnalyzer.getImpliedConstraints(tables);
                else
                    impliedConstraints = new ArrayList<ImpliedForeignKeyConstraint>();

                List<Table> orphans = DbAnalyzer.getOrphans(tables);
                boolean hasOrphans = !orphans.isEmpty() && Dot.getInstance().isValid();

                if (!fineEnabled)
                    System.out.print(".");

                File impliedDotFile = new File(diagramsDir, dotBaseFilespec + ".implied.compact.dot");
                out = new LineWriter(impliedDotFile, Config.DOT_CHARSET);
                boolean hasImplied = DotFormatter.getInstance().writeAllRelationships(db, tables, true, showDetailedTables, stats, out);

                Set<TableColumn> excludedColumns = stats.getExcludedColumns();
                out.close();
                if (hasImplied) {
                    impliedDotFile = new File(diagramsDir, dotBaseFilespec + ".implied.large.dot");
                    out = new LineWriter(impliedDotFile, Config.DOT_CHARSET);
                    DotFormatter.getInstance().writeAllRelationships(db, tables, false, showDetailedTables, stats, out);
                    out.close();
                } else {
                    impliedDotFile.delete();
                }

                out = new LineWriter(new File(outputDir, dotBaseFilespec + ".html"), config.getCharset());
                HtmlRelationshipsPage.getInstance().write(db, diagramsDir, dotBaseFilespec, hasOrphans, hasRealRelationships, hasImplied, excludedColumns, out);
                out.close();

                if (!fineEnabled)
                    System.out.print(".");

                dotBaseFilespec = "utilities";
                out = new LineWriter(new File(outputDir, dotBaseFilespec + ".html"), config.getCharset());
                HtmlOrphansPage.getInstance().write(db, orphans, diagramsDir, out);
                out.close();

                if (!fineEnabled)
                    System.out.print(".");

                out = new LineWriter(new File(outputDir, "index.html"), 64 * 1024, config.getCharset());
                HtmlMainIndexPage.getInstance().write(db, tables, hasOrphans, out);
                out.close();

                if (!fineEnabled)
                    System.out.print(".");

                List<ForeignKeyConstraint> constraints = DbAnalyzer.getForeignKeyConstraints(tables);
                out = new LineWriter(new File(outputDir, "constraints.html"), 256 * 1024, config.getCharset());
                HtmlConstraintsPage constraintIndexFormatter = HtmlConstraintsPage.getInstance();
                constraintIndexFormatter.write(db, constraints, tables, hasOrphans, out);
                out.close();

                if (!fineEnabled)
                    System.out.print(".");

                out = new LineWriter(new File(outputDir, "anomalies.html"), 16 * 1024, config.getCharset());
                HtmlAnomaliesPage.getInstance().write(db, tables, impliedConstraints, hasOrphans, out);
                out.close();

                if (!fineEnabled)
                    System.out.print(".");

                for (HtmlColumnsPage.ColumnInfo columnInfo : HtmlColumnsPage.getInstance().getColumnInfos()) {
                    out = new LineWriter(new File(outputDir, columnInfo.getLocation()), 16 * 1024, config.getCharset());
                    HtmlColumnsPage.getInstance().write(db, tables, columnInfo, hasOrphans, out);
                    out.close();
                }

                // create detailed diagrams

                startDiagrammingDetails = System.currentTimeMillis();
                if (!fineEnabled)
                    System.out.println("(" + (startDiagrammingDetails - startSummarizing) / 1000 + "sec)");
                logger.info("Completed summary in " + (startDiagrammingDetails - startSummarizing) / 1000 + " seconds");
                logger.info("Writing/diagramming details");
                if (!fineEnabled) {
                    System.out.print("Writing/diagramming details");
                }

                HtmlTablePage tableFormatter = HtmlTablePage.getInstance();
                for (Table table : tables) {
                    if (!fineEnabled)
                        System.out.print('.');
                    else
                        logger.fine("Writing details of " + table.getName());

                    out = new LineWriter(new File(outputDir, "tables/" + table.getName() + ".html"), 24 * 1024, config.getCharset());
                    tableFormatter.write(db, table, hasOrphans, outputDir, stats, out);
                    out.close();
                }

                out = new LineWriter(new File(outputDir, "schemaSpy.css"), config.getCharset());
                StyleSheet.getInstance().write(out);
                out.close();
            }


            XmlTableFormatter.getInstance().appendTables(rootNode, tables);

            String xmlName = dbName;

            // some dbNames have path info in the name...strip it
            xmlName = new File(xmlName).getName();

            if (schema != null)
                xmlName += '.' + schema;

            out = new LineWriter(new File(outputDir, xmlName + ".xml"), Config.DOT_CHARSET);
            document.getDocumentElement().normalize();
            DOMUtil.printDOM(document, out);
            out.close();

            // 'try' to make some memory available for the sorting process
            // (some people have run out of memory while RI sorting tables)
            builder = null;
            connection = null;
            document = null;
            factory = null;
            meta = null;
            properties = null;
            rootNode = null;
            urlBuilder = null;

            List<ForeignKeyConstraint> recursiveConstraints = new ArrayList<ForeignKeyConstraint>();

            // create an orderer to be able to determine insertion and deletion ordering of tables
            TableOrderer orderer = new TableOrderer();

            // side effect is that the RI relationships get trashed
            // also populates the recursiveConstraints collection
            List<Table> orderedTables = orderer.getTablesOrderedByRI(db.getTables(), recursiveConstraints);

            out = new LineWriter(new File(outputDir, "insertionOrder.txt"), 16 * 1024, Config.DOT_CHARSET);
            TextFormatter.getInstance().write(orderedTables, false, out);
            out.close();

            out = new LineWriter(new File(outputDir, "deletionOrder.txt"), 16 * 1024, Config.DOT_CHARSET);
            Collections.reverse(orderedTables);
            TextFormatter.getInstance().write(orderedTables, false, out);
            out.close();

            /* we'll eventually want to put this functionality back in with a
             * database independent implementation
            File constraintsFile = new File(outputDir, "removeRecursiveConstraints.sql");
            constraintsFile.delete();
            if (!recursiveConstraints.isEmpty()) {
                out = new LineWriter(constraintsFile, 4 * 1024);
                writeRemoveRecursiveConstraintsSql(recursiveConstraints, schema, out);
                out.close();
            }

            constraintsFile = new File(outputDir, "restoreRecursiveConstraints.sql");
            constraintsFile.delete();

            if (!recursiveConstraints.isEmpty()) {
                out = new LineWriter(constraintsFile, 4 * 1024);
                writeRestoreRecursiveConstraintsSql(recursiveConstraints, schema, out);
                out.close();
            }
            */

            if (config.isHtmlGenerationEnabled()) {
                long end = System.currentTimeMillis();
                if (!fineEnabled)
                    System.out.println("(" + (end - startDiagrammingDetails) / 1000 + "sec)");
                logger.info("Wrote table details in " + (end - startDiagrammingDetails) / 1000 + " seconds");

                if (logger.isLoggable(Level.INFO)) {
                    logger.info("Wrote relationship details of " + tables.size() + " tables/views to directory '" + config.getOutputDir() + "' in " + (end - start) / 1000 + " seconds.");
                    logger.info("View the results by opening " + new File(config.getOutputDir(), "index.html"));
                } else {
                    System.out.println("Wrote relationship details of " + tables.size() + " tables/views to directory '" + config.getOutputDir() + "' in " + (end - start) / 1000 + " seconds.");
                    System.out.println("View the results by opening " + new File(config.getOutputDir(), "index.html"));
                }
            }

            return db;
        } catch (Config.MissingRequiredParameterException missingParam) {
            config.dumpUsage(missingParam.getMessage(), missingParam.isDbTypeSpecific());
            return null;
        }
    }

    /**
     * dumpNoDataMessage
     *
     * @param schema String
     * @param user String
     * @param meta DatabaseMetaData
     */
    private static void dumpNoTablesMessage(String schema, String user, DatabaseMetaData meta, boolean specifiedInclusions) throws SQLException {
        System.out.println();
        System.out.println();
        System.out.println("No tables or views were found in schema '" + schema + "'.");
        List<String> schemas = DbAnalyzer.getSchemas(meta);
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
        for (String unknown : schemas) {
            System.out.print(unknown + " ");
        }

        System.out.println();
        List<String> populatedSchemas = DbAnalyzer.getPopulatedSchemas(meta);
        if (populatedSchemas.isEmpty()) {
            System.out.println("Unable to determine if any of the schemas contain tables/views");
        } else {
            System.out.println("These schemas contain tables/views that user '" + user + "' can see:");
            System.out.println();
            for (String populated : populatedSchemas) {
                System.out.print(" " + populated);
            }
        }
    }

    private Connection getConnection(Config config, String connectionURL,
                      String driverClass, String driverPath) throws FileNotFoundException, IOException {
        if (logger.isLoggable(Level.INFO)) {
            logger.info("Using database properties:");
            logger.info("  " + config.getDbPropertiesLoadedFrom());
        } else {
            System.out.println("Using database properties:");
            System.out.println("  " + config.getDbPropertiesLoadedFrom());
        }

        List<URL> classpath = new ArrayList<URL>();
        List<File> invalidClasspathEntries = new ArrayList<File>();
        StringTokenizer tokenizer = new StringTokenizer(driverPath, File.pathSeparator);
        while (tokenizer.hasMoreTokens()) {
            File pathElement = new File(tokenizer.nextToken());
            if (pathElement.exists())
                classpath.add(pathElement.toURI().toURL());
            else
                invalidClasspathEntries.add(pathElement);
        }

        URLClassLoader loader = new URLClassLoader(classpath.toArray(new URL[classpath.size()]));
        Driver driver = null;
        try {
            driver = (Driver)Class.forName(driverClass, true, loader).newInstance();

            // have to use deprecated method or we won't see messages generated by older drivers
            //java.sql.DriverManager.setLogStream(System.err);
        } catch (Exception exc) {
            System.err.println(exc); // people don't want to see a stack trace...
            System.err.println();
            System.err.print("Failed to load driver '" + driverClass + "'");
            if (classpath.isEmpty())
                System.err.println();
            else
                System.err.println("from: " + classpath);
            if (!invalidClasspathEntries.isEmpty()) {
                if (invalidClasspathEntries.size() == 1)
                    System.err.print("This entry doesn't point to a valid file/directory: ");
                else
                    System.err.print("These entries don't point to valid files/directories: ");
                System.err.println(invalidClasspathEntries);
            }
            System.err.println();
            System.err.println("Use the -dp option to specify the location of the database");
            System.err.println("drivers for your database (usually in a .jar or .zip/.Z).");
            System.err.println();
            throw new ConnectionFailure(exc);
        }

        Properties connectionProperties = config.getConnectionProperties();
        if (config.getUser() != null) {
            connectionProperties.put("user", config.getUser());
        }
        if (config.getPassword() != null) {
            connectionProperties.put("password", config.getPassword());
        } else if (config.isPromptForPasswordEnabled()) {
            connectionProperties.put("password",
                    new String(PasswordReader.getInstance().readPassword("Password: ")));
        }

        Connection connection = null;
        try {
            connection = driver.connect(connectionURL, connectionProperties);
            if (connection == null) {
                System.err.println();
                System.err.println("Cannot connect to this database URL:");
                System.err.println("  " + connectionURL);
                System.err.println("with this driver:");
                System.err.println("  " + driverClass);
                System.err.println();
                System.err.println("Additional connection information may be available in ");
                System.err.println("  " + config.getDbPropertiesLoadedFrom());
                throw new ConnectionFailure("Cannot connect to '" + connectionURL +"' with driver '" + driverClass + "'");
            }
        } catch (UnsatisfiedLinkError badPath) {
            System.err.println();
            System.err.println("Failed to load driver [" + driverClass + "] from classpath " + classpath);
            System.err.println();
            System.err.println("Make sure the reported library (.dll/.lib/.so) from the following line can be");
            System.err.println("found by your PATH (or LIB*PATH) environment variable");
            System.err.println();
            badPath.printStackTrace();
            throw new ConnectionFailure(badPath);
        } catch (Exception exc) {
            System.err.println();
            System.err.println("Failed to connect to database URL [" + connectionURL + "]");
            System.err.println();
            exc.printStackTrace();
            throw new ConnectionFailure(exc);
        }

        return connection;
    }

    /**
     * Currently very DB2-specific
     * @param recursiveConstraints List
     * @param schema String
     * @param out LineWriter
     * @throws IOException
     */
    /* we'll eventually want to put this functionality back in with a
     * database independent implementation
    private static void writeRemoveRecursiveConstraintsSql(List recursiveConstraints, String schema, LineWriter out) throws IOException {
        for (Iterator iter = recursiveConstraints.iterator(); iter.hasNext(); ) {
            ForeignKeyConstraint constraint = (ForeignKeyConstraint)iter.next();
            out.writeln("ALTER TABLE " + schema + "." + constraint.getChildTable() + " DROP CONSTRAINT " + constraint.getName() + ";");
        }
    }
    */

    /**
     * Currently very DB2-specific
     * @param recursiveConstraints List
     * @param schema String
     * @param out LineWriter
     * @throws IOException
     */
    /* we'll eventually want to put this functionality back in with a
     * database independent implementation
    private static void writeRestoreRecursiveConstraintsSql(List recursiveConstraints, String schema, LineWriter out) throws IOException {
        Map ruleTextMapping = new HashMap();
        ruleTextMapping.put(new Character('C'), "CASCADE");
        ruleTextMapping.put(new Character('A'), "NO ACTION");
        ruleTextMapping.put(new Character('N'), "NO ACTION"); // Oracle
        ruleTextMapping.put(new Character('R'), "RESTRICT");
        ruleTextMapping.put(new Character('S'), "SET NULL");  // Oracle

        for (Iterator iter = recursiveConstraints.iterator(); iter.hasNext(); ) {
            ForeignKeyConstraint constraint = (ForeignKeyConstraint)iter.next();
            out.write("ALTER TABLE \"" + schema + "\".\"" + constraint.getChildTable() + "\" ADD CONSTRAINT \"" + constraint.getName() + "\"");
            StringBuffer buf = new StringBuffer();
            for (Iterator columnIter = constraint.getChildColumns().iterator(); columnIter.hasNext(); ) {
                buf.append("\"");
                buf.append(columnIter.next());
                buf.append("\"");
                if (columnIter.hasNext())
                    buf.append(",");
            }
            out.write(" FOREIGN KEY (" + buf.toString() + ")");
            out.write(" REFERENCES \"" + schema + "\".\"" + constraint.getParentTable() + "\"");
            buf = new StringBuffer();
            for (Iterator columnIter = constraint.getParentColumns().iterator(); columnIter.hasNext(); ) {
                buf.append("\"");
                buf.append(columnIter.next());
                buf.append("\"");
                if (columnIter.hasNext())
                    buf.append(",");
            }
            out.write(" (" + buf.toString() + ")");
            out.write(" ON DELETE ");
            out.write(ruleTextMapping.get(new Character(constraint.getDeleteRule())).toString());
            out.write(" ON UPDATE ");
            out.write(ruleTextMapping.get(new Character(constraint.getUpdateRule())).toString());
            out.writeln(";");
        }
    }
    */

    private static void yankParam(List<String> args, String paramId) {
        int paramIndex = args.indexOf(paramId);
        if (paramIndex >= 0) {
            args.remove(paramIndex);
            args.remove(paramIndex);
        }
    }
}
