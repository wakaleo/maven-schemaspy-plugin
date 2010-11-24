package net.sourceforge.schemaspy.model;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.regex.*;

public class Database {
    private final String databaseName;
    private final String schema;
    private final String description;
    private final Map tables = new HashMap();
    private final Map views = new HashMap();
    private final DatabaseMetaData meta;
    private final Connection connection;
    private final String connectTime = new SimpleDateFormat("EEE MMM dd HH:mm z yyyy").format(new Date());
    private Set sqlKeywords;
    private Pattern invalidIdentifierPattern;

    public Database(Connection connection, DatabaseMetaData meta, String name, String schema, String description, Properties properties, Pattern include, int maxThreads) throws SQLException, MissingResourceException {
        this.connection = connection;
        this.meta = meta;
        this.databaseName = name;
        this.schema = schema;
        this.description = description;
        initTables(schema, meta, properties, include, maxThreads);
        initViews(schema, meta, properties, include);
        connectTables(meta);
    }

    public String getName() {
        return databaseName;
    }

    public String getSchema() {
        return schema;
    }
    
    /**
     * @return null if a description wasn't specified.
     */
    public String getDescription() {
        return description;
    }

    public Collection getTables() {
        return tables.values();
    }

    public Collection getViews() {
        return views.values();
    }

    public Connection getConnection() {
        return connection;
    }

    public DatabaseMetaData getMetaData() {
        return meta;
    }

    public String getConnectTime() {
        return connectTime;
    }

    public String getDatabaseProduct() {
        try {
            return meta.getDatabaseProductName() + " - " + meta.getDatabaseProductVersion();
        } catch (SQLException exc) {
            return "";
        }
    }

    private void initTables(String schema, final DatabaseMetaData metadata, final Properties properties, final Pattern include, final int maxThreads) throws SQLException {
        String[] types = {"TABLE"};
        ResultSet rs = null;

        // "macro" to validate that a table is somewhat valid
        class TableValidator {
            boolean isValid(ResultSet rs) throws SQLException {
                // some databases (MySQL) return more than we wanted
                if (!rs.getString("TABLE_TYPE").equalsIgnoreCase("TABLE"))
                    return false;
                
                // Oracle 10g introduced problematic flashback tables
                // with bizarre illegal names
                String tableName = rs.getString("TABLE_NAME");
                if (tableName.indexOf("$") != -1)
                    return false;
    
                if (!include.matcher(tableName).matches())
                    return false;
                
                return true;
            }
        }
        TableValidator tableValidator = new TableValidator();
        
        try {
            // creating tables takes a LONG time (based on JProbe analysis).
            // it's actually DatabaseMetaData.getIndexInfo() that's the pig.

            rs = metadata.getTables(null, schema, "%", types);

            TableCreator creator;
            if (maxThreads == 1) {
                creator = new TableCreator();
            } else {
                creator = new ThreadedTableCreator(maxThreads);

                // "prime the pump" so if there's a database problem we'll probably see it now
                // and not in a secondary thread
                while (rs.next()) {
                    if (tableValidator.isValid(rs)) {
                        new TableCreator().create(rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME"), getOptionalString(rs, "REMARKS"), properties);
                        break;
                    }
                }
            }

            while (rs.next()) {
                if (tableValidator.isValid(rs)) {
                    creator.create(rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME"), getOptionalString(rs, "REMARKS"), properties);
                }
            }

            creator.join();
        } finally {
            if (rs != null)
                rs.close();
        }

        initCheckConstraints(properties);
        initTableIds(properties);
        initIndexIds(properties);
        initTableComments(properties);
        initColumnComments(properties);
    }
    
    /**
     * Some databases don't play nice with their metadata.
     * E.g. Oracle doesn't have a REMARKS column at all.
     * This method ignores those types of failures, replacing them with null.
     */
    public String getOptionalString(ResultSet rs, String columnName)
    {
        try {
            return rs.getString(columnName);
        } catch (SQLException ignore) {
            return null;
        }
    }

    private void initCheckConstraints(Properties properties) throws SQLException {
        String sql = properties.getProperty("selectCheckConstraintsSql");
        if (sql != null) {
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                stmt = prepareStatement(sql, null);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    Table table = (Table)tables.get(tableName.toUpperCase());
                    if (table != null)
                        table.addCheckConstraint(rs.getString("constraint_name"), rs.getString("text"));
                }
            } catch (SQLException sqlException) {
                System.err.println();
                System.err.println(sql);
                throw sqlException;
            } finally {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            }
        }
    }

    private void initTableIds(Properties properties) throws SQLException {
        String sql = properties.getProperty("selectTableIdsSql");
        if (sql != null) {
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                stmt = prepareStatement(sql, null);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    Table table = (Table)tables.get(tableName.toUpperCase());
                    if (table != null)
                        table.setId(rs.getObject("table_id"));
                }
            } catch (SQLException sqlException) {
                System.err.println();
                System.err.println(sql);
                throw sqlException;
            } finally {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            }
        }
    }

    private void initIndexIds(Properties properties) throws SQLException {
        String sql = properties.getProperty("selectIndexIdsSql");
        if (sql != null) {
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                stmt = prepareStatement(sql, null);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    Table table = (Table)tables.get(tableName.toUpperCase());
                    if (table != null) {
                        TableIndex index = table.getIndex(rs.getString("index_name"));
                        if (index != null)
                            index.setId(rs.getObject("index_id"));
                    }
                }
            } catch (SQLException sqlException) {
                System.err.println();
                System.err.println(sql);
                throw sqlException;
            } finally {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            }
        }
    }
    
    private void initTableComments(Properties properties) throws SQLException {
        String sql = properties.getProperty("selectTableCommentsSql");
        if (sql != null) {
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                stmt = prepareStatement(sql, null);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    Table table = (Table)tables.get(tableName.toUpperCase());
                    if (table != null)
                        table.setComments(rs.getString("comments"));
                }
            } catch (SQLException sqlException) {
                // don't die just because this failed
                System.err.println();
                System.err.println("Failed to retrieve table comments: " + sqlException);
                System.err.println(sql);
            } finally {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            }
        }
    }
    
    private void initColumnComments(Properties properties) throws SQLException {
        String sql = properties.getProperty("selectColumnCommentsSql");
        if (sql != null) {
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                stmt = prepareStatement(sql, null);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    Table table = (Table)tables.get(tableName.toUpperCase());
                    if (table != null) {
                        TableColumn column = table.getColumn(rs.getString("column_name"));
                        if (column != null)
                            column.setComments(rs.getString("comments"));
                    }
                }
            } catch (SQLException sqlException) {
                // don't die just because this failed
                System.err.println();
                System.err.println("Failed to retrieve column comments: " + sqlException);
                System.err.println(sql);
            } finally {
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
            }
        }
    }
    
    /**
     * Create a <code>PreparedStatement</code> from the specified SQL.
     * The SQL can contain these named parameters (but <b>not</b> question marks).
     * <ol>
     * <li>:schema - replaced with the name of the schema
     * <li>:owner - alias for :schema
     * <li>:table - replaced with the name of the table
     * </ol>
     * @param sql String - SQL without question marks
     * @param tableName String - <code>null</code> if the statement doesn't deal with <code>Table</code>-level details.
     * @throws SQLException
     * @return PreparedStatement
     */
    public PreparedStatement prepareStatement(String sql, String tableName) throws SQLException {
        StringBuffer sqlBuf = new StringBuffer(sql);
        List sqlParams = getSqlParams(sqlBuf, tableName); // modifies sqlBuf
        PreparedStatement stmt = getConnection().prepareStatement(sqlBuf.toString());

        try {
            for (int i = 0; i < sqlParams.size(); ++i) {
                stmt.setString(i + 1, sqlParams.get(i).toString());
            }
        } catch (SQLException exc) {
            stmt.close();
            throw exc;
        }

        return stmt;
    }

    /**
     * Return an uppercased <code>Set</code> of all SQL keywords used by a database
     * 
     * @return
     * @throws SQLException
     */
    public Set getSqlKeywords() throws SQLException {
        if (sqlKeywords == null) {
            // from http://www.contrib.andrew.cmu.edu/~shadow/sql/sql1992.txt:
            String[] sql92Keywords =            
                ("ADA" +
                "| C | CATALOG_NAME | CHARACTER_SET_CATALOG | CHARACTER_SET_NAME" +
                "| CHARACTER_SET_SCHEMA | CLASS_ORIGIN | COBOL | COLLATION_CATALOG" +
                "| COLLATION_NAME | COLLATION_SCHEMA | COLUMN_NAME | COMMAND_FUNCTION | COMMITTED" +
                "| CONDITION_NUMBER | CONNECTION_NAME | CONSTRAINT_CATALOG | CONSTRAINT_NAME" +
                "| CONSTRAINT_SCHEMA | CURSOR_NAME" +
                "| DATA | DATETIME_INTERVAL_CODE | DATETIME_INTERVAL_PRECISION | DYNAMIC_FUNCTION" +
                "| FORTRAN" +
                "| LENGTH" +
                "| MESSAGE_LENGTH | MESSAGE_OCTET_LENGTH | MESSAGE_TEXT | MORE | MUMPS" +
                "| NAME | NULLABLE | NUMBER" +
                "| PASCAL | PLI" +
                "| REPEATABLE | RETURNED_LENGTH | RETURNED_OCTET_LENGTH | RETURNED_SQLSTATE" +
                "| ROW_COUNT" +
                "| SCALE | SCHEMA_NAME | SERIALIZABLE | SERVER_NAME | SUBCLASS_ORIGIN" +
                "| TABLE_NAME | TYPE" +
                "| UNCOMMITTED | UNNAMED" +
                "| ABSOLUTE | ACTION | ADD | ALL | ALLOCATE | ALTER | AND" +
                "| ANY | ARE | AS | ASC" +
                "| ASSERTION | AT | AUTHORIZATION | AVG" +
                "| BEGIN | BETWEEN | BIT | BIT_LENGTH | BOTH | BY" +
                "| CASCADE | CASCADED | CASE | CAST | CATALOG | CHAR | CHARACTER | CHAR_LENGTH" +
                "| CHARACTER_LENGTH | CHECK | CLOSE | COALESCE | COLLATE | COLLATION" +
                "| COLUMN | COMMIT | CONNECT | CONNECTION | CONSTRAINT" +
                "| CONSTRAINTS | CONTINUE" +
                "| CONVERT | CORRESPONDING | COUNT | CREATE | CROSS | CURRENT" +
                "| CURRENT_DATE | CURRENT_TIME | CURRENT_TIMESTAMP | CURRENT_USER | CURSOR" +
                "| DATE | DAY | DEALLOCATE | DEC | DECIMAL | DECLARE | DEFAULT | DEFERRABLE" +
                "| DEFERRED | DELETE | DESC | DESCRIBE | DESCRIPTOR | DIAGNOSTICS" +
                "| DISCONNECT | DISTINCT | DOMAIN | DOUBLE | DROP" +
                "| ELSE | END | END-EXEC | ESCAPE | EXCEPT | EXCEPTION" +
                "| EXEC | EXECUTE | EXISTS" +
                "| EXTERNAL | EXTRACT" +
                "| FALSE | FETCH | FIRST | FLOAT | FOR | FOREIGN | FOUND | FROM | FULL" +
                "| GET | GLOBAL | GO | GOTO | GRANT | GROUP" +
                "| HAVING | HOUR" +
                "| IDENTITY | IMMEDIATE | IN | INDICATOR | INITIALLY | INNER | INPUT" +
                "| INSENSITIVE | INSERT | INT | INTEGER | INTERSECT | INTERVAL | INTO | IS" +
                "| ISOLATION" +
                "| JOIN" +
                "| KEY" +
                "| LANGUAGE | LAST | LEADING | LEFT | LEVEL | LIKE | LOCAL | LOWER" +
                "| MATCH | MAX | MIN | MINUTE | MODULE | MONTH" +
                "| NAMES | NATIONAL | NATURAL | NCHAR | NEXT | NO | NOT | NULL" +
                "| NULLIF | NUMERIC" +
                "| OCTET_LENGTH | OF | ON | ONLY | OPEN | OPTION | OR" +
                "| ORDER | OUTER" +
                "| OUTPUT | OVERLAPS" +
                "| PAD | PARTIAL | POSITION | PRECISION | PREPARE | PRESERVE | PRIMARY" +
                "| PRIOR | PRIVILEGES | PROCEDURE | PUBLIC" +
                "| READ | REAL | REFERENCES | RELATIVE | RESTRICT | REVOKE | RIGHT" +
                "| ROLLBACK | ROWS" +
                "| SCHEMA | SCROLL | SECOND | SECTION | SELECT | SESSION | SESSION_USER | SET" +
                "| SIZE | SMALLINT | SOME | SPACE | SQL | SQLCODE | SQLERROR | SQLSTATE" +
                "| SUBSTRING | SUM | SYSTEM_USER" +
                "| TABLE | TEMPORARY | THEN | TIME | TIMESTAMP | TIMEZONE_HOUR | TIMEZONE_MINUTE" +
                "| TO | TRAILING | TRANSACTION | TRANSLATE | TRANSLATION | TRIM | TRUE" +
                "| UNION | UNIQUE | UNKNOWN | UPDATE | UPPER | USAGE | USER | USING" +
                "| VALUE | VALUES | VARCHAR | VARYING | VIEW" +
                "| WHEN | WHENEVER | WHERE | WITH | WORK | WRITE" +
                "| YEAR" +
                "| ZONE").split("|,\\s*");

            String[] nonSql92Keywords = getMetaData().getSQLKeywords().toUpperCase().split(",\\s*");

            sqlKeywords = new HashSet();
            sqlKeywords.addAll(Arrays.asList(sql92Keywords));
            sqlKeywords.addAll(Arrays.asList(nonSql92Keywords));
        }
        
        return sqlKeywords;
    }
    
    public String getQuotedIdentifier(String id) throws SQLException {
        // look for any character that isn't valid (then matcher.find() returns true)
        Matcher matcher = getInvalidIdentifierPattern().matcher(id);
        
        boolean quotesRequired = matcher.find() || getSqlKeywords().contains(id.toUpperCase());
        
        if (quotesRequired) {
            // name contains something that must be quoted
            String quote = getMetaData().getIdentifierQuoteString().trim();
            return quote + id + quote;
        } else {
            // no quoting necessary
            return id;
        }
    }
    
    /**
     * Return a <code>Pattern</code> whose matcher will return <code>true</code>
     * when run against an identifier that contains a character that is not
     * acceptable by the database without being quoted.
     */
    private Pattern getInvalidIdentifierPattern() throws SQLException {
        if (invalidIdentifierPattern == null) {
            String validChars = "a-zA-Z0-9_";
            String reservedRegexChars = "-&^";
            String extraValidChars = getMetaData().getExtraNameCharacters();
            for (int i = 0; i < extraValidChars.length(); ++i) {
                char ch = extraValidChars.charAt(i);
                if (reservedRegexChars.indexOf(ch) >= 0)
                    validChars += "\\";
                validChars += ch;
            }

            invalidIdentifierPattern = Pattern.compile("[^" + validChars + "]");
        }
        
        return invalidIdentifierPattern;
    }
    
    /**
     * Replaces named parameters in <code>sql</code> with question marks and
     * returns appropriate matching values in the returned <code>List</code> of <code>String</code>s.
     *
     * @param sql StringBuffer input SQL with named parameters, output named params are replaced with ?'s.
     * @param tableName String
     * @return List of Strings
     *
     * @see #prepareStatement(String, String)
     */
    private List getSqlParams(StringBuffer sql, String tableName) {
        Map namedParams = new HashMap();
        String schema = getSchema();
        if (schema == null)
            schema = getName(); // some 'schema-less' db's treat the db name like a schema (unusual case)
        namedParams.put(":schema", schema);
        namedParams.put(":owner", schema); // alias for :schema
        if (tableName != null) {
            namedParams.put(":table", tableName);
            namedParams.put(":view", tableName); // alias for :table
        }

        List sqlParams = new ArrayList();
        int nextColon = sql.indexOf(":");
        while (nextColon != -1) {
            String paramName = new StringTokenizer(sql.substring(nextColon), " ,").nextToken();
            String paramValue = (String)namedParams.get(paramName);
            if (paramValue == null)
                throw new IllegalArgumentException("Unexpected named parameter '" + paramName + "' found in SQL '" + sql + "'");
            sqlParams.add(paramValue);
            sql.replace(nextColon, nextColon + paramName.length(), "?"); // replace with a ?
            nextColon = sql.indexOf(":", nextColon);
        }

        return sqlParams;
    }


    private void initViews(String schema, DatabaseMetaData metadata, Properties properties, Pattern include) throws SQLException {
        String[] types = {"VIEW"};
        ResultSet rs = null;

        try {
            rs = metadata.getTables(null, schema, "%", types);

            while (rs.next()) {
                if (rs.getString("TABLE_TYPE").equals("VIEW")) {  // some databases (MySQL) return more than we wanted
                    System.out.print('.');
                    
                    Table view = new View(this, rs, metadata, properties.getProperty("selectViewSql"));
                    if (include.matcher(view.getName()).matches())
                        views.put(view.getName().toUpperCase(), view);
                }
            }
        } finally {
            if (rs != null)
                rs.close();
        }
    }

    private void connectTables(DatabaseMetaData metadata) throws SQLException {
        Iterator iter = tables.values().iterator();
        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            table.connectForeignKeys(tables, metadata);
        }
    }

    /**
     * Single-threaded implementation of a class that creates tables
     */
    private class TableCreator {
        /**
         * Create a table and put it into <code>tables</code>
         */
        void create(String schemaName, String tableName, String remarks, Properties properties) throws SQLException {
            createImpl(schemaName, tableName, remarks, properties);
        }

        protected void createImpl(String schemaName, String tableName, String remarks, Properties properties) throws SQLException {
            Table table = new Table(Database.this, schemaName, tableName, remarks, meta, properties);
            tables.put(table.getName().toUpperCase(), table);
            System.out.print('.');
        }

        /**
         * Wait for all of the tables to be created.
         * By default this does nothing since this implementation isn't threaded.
         */
        void join() {
        }
    }

    /**
     * Multi-threaded implementation of a class that creates tables
     */
    private class ThreadedTableCreator extends TableCreator {
        private final Set threads = new HashSet();
        private final int maxThreads;

        ThreadedTableCreator(int maxThreads) {
            this.maxThreads = maxThreads;
        }

        void create(final String schemaName, final String tableName, final String remarks, final Properties properties) throws SQLException {
            Thread runner = new Thread() {
                public void run() {
                    try {
                        createImpl(schemaName, tableName, remarks, properties);
                    } catch (SQLException exc) {
                        exc.printStackTrace(); // nobody above us in call stack...dump it here
                    } finally {
                        synchronized (threads) {
                            threads.remove(this);
                            threads.notify();
                        }
                    }
                }
            };

            synchronized (threads) {
                // wait for enough 'room'
                while (threads.size() >= maxThreads) {
                    try {
                        threads.wait();
                    } catch (InterruptedException interrupted) {
                    }
                }

                threads.add(runner);
            }

            runner.start();
        }

        /**
         * Wait for all of the started threads to complete
         */
        public void join() {
            while (true) {
                Thread thread;

                synchronized (threads) {
                    Iterator iter = threads.iterator();
                    if (!iter.hasNext())
                        break;

                    thread = (Thread)iter.next();
                }

                try {
                    thread.join();
                } catch (InterruptedException exc) {
                }
            }
        }
    }
}
