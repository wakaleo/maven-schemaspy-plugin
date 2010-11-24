package net.sourceforge.schemaspy.view;

import java.io.*;

public class ImageWriter {
    private static ImageWriter instance = new ImageWriter();

    private ImageWriter() {
    }

    public static ImageWriter getInstance() {
        return instance;
    }

    public void writeImages(File outputDir) throws IOException {
        new File(outputDir, "images").mkdir();

        writeImage("/images/tabLeft.gif", outputDir);
        writeImage("/images/tabRight.gif", outputDir);
        writeImage("/images/background.gif", outputDir);
    }

    private void writeImage(String image, File outputDir) throws IOException {
        InputStream in = ImageWriter.class.getResourceAsStream(image);
        byte[] buf = new byte[4096];

        FileOutputStream out = new FileOutputStream(new File(outputDir, image));
        int numBytes = 0;
        while ((numBytes = in.read(buf)) != -1) {
            out.write(buf, 0, numBytes);
        }
        in.close();
        out.close();
    }
}
