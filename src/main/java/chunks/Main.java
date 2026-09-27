package chunks;

import java.io.FileOutputStream;
import java.io.IOException;

/** Generates the sample input file used by the chunking examples. */
public class Main {
    public static void main(String[] args) {
        final long oneGb = 1024L * 1024L * 1024L;
        final int chunkSize = 1024 * 1024;
        byte[] buffer = new byte[chunkSize];

        try {
            ProjectPaths.ensureOutputDirectories();
            try (FileOutputStream output = new FileOutputStream(ProjectPaths.INPUT_FILE.toFile())) {
                for (long written = 0; written < oneGb; written += chunkSize) {
                    output.write(buffer, 0, (int) Math.min(chunkSize, oneGb - written));
                }
            }
            System.out.println("1GB file generated at " + ProjectPaths.INPUT_FILE + ".");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
