package net.sourceforge.schemaspy.util;

import java.io.*;
import java.util.regex.*;

public class Dot {
    private static Dot instance = new Dot();
    private final Version version;
    private final Version supportedVersion = new Version("2.2.1");
    private final Version badVersion = new Version("2.4");

    private Dot() {
        String versionText = null;
        try {
            // dot -V should return something similar to:
            //  dot version 2.8 (Fri Feb  3 22:38:53 UTC 2006)
            // or sometimes something like:
            //  dot - Graphviz version 2.9.20061004.0440 (Wed Oct 4 21:01:52 GMT 2006)
            String dotCommand = "dot -V";
            Process process = Runtime.getRuntime().exec(dotCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String versionLine = reader.readLine();
            
            // look for a number followed numbers or dots
            Matcher matcher = Pattern.compile("[0-9][0-9.]+").matcher(versionLine);
            if (matcher.find()) {
                versionText = matcher.group();
            } else {
                System.err.println();
                System.err.println("Invalid dot configuration detected.  '" + dotCommand + "' returned:");
                System.err.println("   " + versionLine);
            }
        } catch (Exception validDotDoesntExist) {
        }

        version = new Version(versionText);
    }

    public static Dot getInstance() {
        return instance;
    }

    public boolean exists() {
        return version.toString() != null;
    }

    public Version getVersion() {
        return version;
    }

    public boolean isValid() {
        return exists() && (getVersion().equals(supportedVersion) || getVersion().compareTo(badVersion) > 0);
    }

    public String getSupportedVersions() {
        return "dot version " + supportedVersion + " or versions greater than " + badVersion;
    }

    public boolean supportsCenteredEastWestEdges() {
        return getVersion().compareTo(new Version("2.6")) >= 0;
    }

    public void generateGraph(File dotFile, File graphFile) throws DotFailure {
        // this one is for executing.  it can (hopefully) deal with funky things in filenames.
        String[] dotParams = new String[] {"dot", "-Tpng", dotFile.toString(), "-o" + graphFile};
        // this one is for display purposes ONLY.
        String commandLine = getDisplayableCommand(dotParams);
        try {
            Process process = Runtime.getRuntime().exec(dotParams);
            new ProcessOutputReader(commandLine, process.getErrorStream()).start();
            new ProcessOutputReader(commandLine, process.getInputStream()).start();
            int rc = process.waitFor();
            if (rc != 0)
                throw new DotFailure("'" + commandLine + "' failed with return code " + rc);
            if (!graphFile.exists())
                throw new DotFailure("'" + commandLine + "' failed to create output file");
        } catch (InterruptedException interrupted) {
            interrupted.printStackTrace();
        } catch (DotFailure failed) {
            graphFile.delete();
            throw failed;
        } catch (IOException failed) {
            graphFile.delete();
            throw new DotFailure("'" + commandLine + "' failed with exception " + failed);
        }
    }

    /**
     * Create html image maps from the specified .dot file
     */
    public void writeMap(File dotFile, LineWriter out) throws DotFailure {
        BufferedReader mapReader = null;
        // this one is for executing.  it can (hopefully) deal with funky things in filenames.
        String[] dotParams = new String[] {"dot", "-Tcmapx", dotFile.toString()};
        // this one is for display purposes ONLY.
        String commandLine = getDisplayableCommand(dotParams);

        try {
            Process process = Runtime.getRuntime().exec(dotParams);
            new ProcessOutputReader(commandLine, process.getErrorStream()).start();
            mapReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = mapReader.readLine()) != null)
                out.writeln(line);
            int rc = process.waitFor();
            if (rc != 0)
                throw new DotFailure("'" + commandLine + "' failed with return code " + rc);
        } catch (InterruptedException interrupted) {
            interrupted.printStackTrace();
        } catch (DotFailure failed) {
            throw failed;
        } catch (IOException failed) {
            throw new DotFailure("'" + commandLine + "' failed with exception " + failed);
        } finally {
            if (mapReader != null) {
                try {
                    mapReader.close();
                } catch (IOException ignore) {}
            }
        }
    }

    public class DotFailure extends IOException {
        private static final long serialVersionUID = 3833743270181351987L;

        public DotFailure(String msg) {
            super(msg);
        }
    }

    private static String getDisplayableCommand(String[] command) {
        StringBuffer displayable = new StringBuffer();
        for (int i = 0; i < command.length; ++i) {
            displayable.append(command[i]);
            if (i + 1 < command.length)
                displayable.append(' ');
        }
        return displayable.toString();
    }

    private static class ProcessOutputReader extends Thread {
        private final BufferedReader processReader;
        private final String command;

        ProcessOutputReader(String command, InputStream processStream) {
            processReader = new BufferedReader(new InputStreamReader(processStream));
            this.command = command;
            setDaemon(true);
        }

        public void run() {
            try {
                String line;
                while ((line = processReader.readLine()) != null) {
                    // don't report port id unrecognized or unrecognized port
                    if (line.indexOf("unrecognized") == -1 && line.indexOf("port") == -1)
                        System.err.println(command + ": " + line);
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