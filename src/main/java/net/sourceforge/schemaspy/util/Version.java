package net.sourceforge.schemaspy.util;

import java.util.*;

/**
 * Simple class that allows logical comparisons between "dotted" versions of products.
 *
 * e.g. version 2.1.4 should be less than version 2.1.10.
 *
 * @author John Currier
 * @version 1.0
 */
public class Version implements Comparable {
    private final List segments = new ArrayList();
    private final String asString;
    private final int hashCode;

    public Version(String version) {
        asString = version;
        int hash = 0;
        if (version != null) {
            StringTokenizer tokenizer = new StringTokenizer(version, ". -_");

            while (tokenizer.hasMoreTokens()) {
                Integer segment = new Integer(tokenizer.nextToken());
                segments.add(segment);
                hash += segment.intValue();
            }
        }

        hashCode = hash;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     */
    public int compareTo(Object version) {
        Version other = (Version)version;
        int size = Math.min(segments.size(), other.segments.size());
        for (int i = 0; i < size; ++i) {
            Integer thisSegment = (Integer)segments.get(i);
            Integer otherSegment = (Integer)other.segments.get(i);
            int result = thisSegment.compareTo(otherSegment);
            if (result != 0)
                return result;
        }

        if (segments.size() == other.segments.size())
            return 0;
        if (segments.size() > other.segments.size())
            return 1;
        return -1;
    }

    public boolean equals(Object other) {
        if (other == null || !(other instanceof Version))
            return false;
        return compareTo(other) == 0;
    }

    public int hashCode() {
        return hashCode;
    }

    public String toString() {
        return asString;
    }
}
