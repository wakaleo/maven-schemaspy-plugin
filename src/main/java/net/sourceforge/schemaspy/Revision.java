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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author John Currier
 */
public class Revision {
    private static String rev = "Unknown";
    private static final String resourceName = "/schemaSpy.rev";

    static {
        initialize();
    }

    private static void initialize() {
        InputStream in = null;
        BufferedReader reader = null;

        try {
            in = Revision.class.getResourceAsStream(resourceName);

            if (in != null) {
                reader = new BufferedReader(new InputStreamReader(in));
                try {
                    rev = reader.readLine();
                } catch (IOException exc) {
                }
            }
        } finally {
            try {
                if (reader != null)
                    reader.close();
                else if (in != null)
                    in.close();
            } catch (IOException ignore) {}
        }
    }

    @Override
    public String toString() {
        return rev;
    }

    public static void main(String[] args) throws IOException {
        File entriesFile = new File(".svn", "entries");
        BufferedReader entries = new BufferedReader(new FileReader(entriesFile));
        entries.readLine(); // lines
        entries.readLine(); // blank
        entries.readLine(); // type
        String revision = entries.readLine(); // rev
        entries.close();

        String buildDir = "output";
        if (args.length < 1)
            buildDir = args[0];
        File revFile = new File(buildDir, resourceName);
        FileWriter out = new FileWriter(revFile);
        out.write(revision);
        out.close();

        initialize();
        System.out.println("Subversion revision " + new Revision());
    }
}