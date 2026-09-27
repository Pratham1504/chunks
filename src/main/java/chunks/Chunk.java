package chunks;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

class ChunkRecords {
    public int id;
    public long position;
    public long size;
    public long checksum;

    public ChunkRecords() {}

    public ChunkRecords(int id, long position, long size, long checksum) {
        this.id = id;
        this.position = position;
        this.size = size;
        this.checksum = checksum;
    }
}

public class Chunk {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        int chunk_size = 1024 * 1024;
        CRC32 crc = new CRC32();
        List<ChunkRecords> chunks = new ArrayList<>();
        try {
            ProjectPaths.ensureOutputDirectories();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (InputStream in = new FileInputStream(ProjectPaths.INPUT_FILE.toFile())) {
            byte[] buffer = new byte[chunk_size];
            int bytesRead;
            int chunkId = 0;
            long position = 0;

            while ((bytesRead = in.read(buffer)) !=-1) {
                byte[] actualData = Arrays.copyOf(buffer, bytesRead);
                Files.write(ProjectPaths.chunkPath(chunkId), actualData);
                position += bytesRead;
                crc.update(buffer, 0, bytesRead);
                System.out.println("chunck ID:" + chunkId + ", position: " + position + ", Length: " + actualData.length + ", CheckSum: " + crc.getValue());
                chunks.add(new ChunkRecords(chunkId, position, actualData.length, crc.getValue()));
                chunkId++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Printing into record file
        try (FileWriter fw = new FileWriter(ProjectPaths.MANIFEST_FILE.toFile())) {
            fw.write("[\n");
            for (int i = 0; i < chunks.size(); i++) {
                ChunkRecords record = chunks.get(i);
                fw.write("  {\n");
                fw.write("    \"id\": " + record.id + ",\n");
                fw.write("    \"position\": " + record.position + ",\n");
                fw.write("    \"size\": " + record.size + ",\n");
                fw.write("    \"checksum\": " + record.checksum + "\n");
                fw.write("  }");
                if (i < chunks.size() - 1) {
                    fw.write(",\n");
                } else {
                    fw.write("\n");
                }
            }
            fw.write("]\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        // printing the time taken by code
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Time taken: " + duration + " ms");

    }
}
