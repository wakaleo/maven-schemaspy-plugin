package net.sourceforge.schemaspy.util;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

public class DOMUtil {
    public static void printDOM(Node node, LineWriter out) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer xformer;
        boolean indentSpecified = false;

        // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
        // for issues about transformations in Java 5.x
        try {
            // won't work pre-5.x
            factory.setAttribute("indent-number", new Integer(3));
            indentSpecified = true;
        } catch (IllegalArgumentException factoryDoesntSupportIndentNumber) {
        }

        xformer = factory.newTransformer();
        xformer.setOutputProperty(OutputKeys.INDENT, "yes");
        if (!indentSpecified)
            xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");

        xformer.transform(new DOMSource(node), new StreamResult(out));
    }

    /**
     * Append the specified key/value pair of attributes to the <code>Node</code>.
     * @param node Node
     * @param name String
     * @param value String
     */
    public static void appendAttribute(Node node, String name, String value) {
        Node attribute = node.getOwnerDocument().createAttribute(name);
        attribute.setNodeValue(value);
        node.getAttributes().setNamedItem(attribute);
    }
}
