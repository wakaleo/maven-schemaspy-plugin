package net.sourceforge.schemaspy.util;

import java.io.*;
import java.util.*;

public class ConnectionURLBuilder {
    private final String type;
    private final String description;
    private final String connectionURL;
    private String dbName = null;
    private final List params = new ArrayList();
    private final List descriptions = new ArrayList();

    /**
     * args is null if you just need a list of params that this type of connection supports
     * @param args
     * @param properties
     */
    public ConnectionURLBuilder(String databaseType, List args, Properties properties) {
        this.type = databaseType;

        StringBuffer url = new StringBuffer();
        boolean inParam = false;

        StringTokenizer tokenizer = new StringTokenizer(properties.getProperty("connectionSpec"), "<>", true);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.equals("<")) {
                inParam = true;
            } else if (token.equals(">")) {
                inParam = false;
            } else {
                if (inParam) {
                    String paramValue = getParam(args, token, properties);
                    if (token.equals("db") || token.equals("-db"))
                        dbName = paramValue;
                    url.append(paramValue);
                } else
                    url.append(token);
            }
        }

        connectionURL = url.toString();

        if (dbName == null) {
            dbName = connectionURL;
        }

        description = properties.getProperty("description");
    }

    public String getConnectionURL() {
        return connectionURL;
    }

    public String getDbName() {
        return dbName;
    }

    public List getParams() {
        return params;
    }

    public List getParamDescriptions() {
        return descriptions;
    }

    private String getParam(List args, String paramName, Properties properties) {
        String param = null;
        int paramIndex = args != null ? args.indexOf("-" + paramName) : -1;
        String description = properties.getProperty(paramName);

        params.add("-" + paramName);
        descriptions.add(description);

        if (args != null) {
            if (paramIndex < 0) {
                if (description != null)
                    description = "(" + description + ") ";
                throw new IllegalArgumentException("Parameter '-" + paramName + "' " + (description != null ? description : "") + "missing.\nIt is required for the specified database type.");
            }
            args.remove(paramIndex);
            param = args.get(paramIndex).toString();
            args.remove(paramIndex);
        }

        return param;
    }

    public void dumpUsage() {
        System.out.println(" " + new File(type).getName() + ":");
        System.out.println("   " + description);
        List params = getParams();
        List paramDescriptions = getParamDescriptions();

        for (int i = 0; i < params.size(); ++i) {
            String param = params.get(i).toString();
            String paramDescription = (String)paramDescriptions.get(i);
            System.out.println("   " + param + " " + (paramDescription != null ? "  \t" + paramDescription : ""));
        }
    }
}
