package chunks;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.CRC32;
import javax.json.*;
import javax.json.stream.JsonGenerator;

public class ChunkMultithreading {
    public static void main(String[] args) {
        //recording the start time.
        long startTime = System.currentTimeMillis();

        int chunk_size = 1024 * 1024;

        //fixed thread pool of 10 threads
        ExecutorService executor = Executors.newFixedThreadPool(5);
        //futures will hold our record data.
        List<Future<ChunkRecords>> futures = new ArrayList<>();
        String fileName = ProjectPaths.INPUT_FILE.toString();

        try {
            ProjectPaths.ensureOutputDirectories();
            long fileSize = Files.size(Paths.get(fileName));
            int totalChunks = (int) ((fileSize + chunk_size - 1) / chunk_size);

            for (int chunkId = 0; chunkId < totalChunks; chunkId++) {
                int id = chunkId;
                long position = (long) id * chunk_size;
                int size = (int) Math.min(chunk_size, fileSize - position);

                futures.add(executor.submit(() -> {
                    try (RandomAccessFile raf = new RandomAccessFile(fileName, "r")) {
                        byte[] buffer = new byte[size];
                        raf.seek(position);
                        raf.readFully(buffer);

                        CRC32 crc = new CRC32();
                        crc.update(buffer, 0, size);
                        Files.write(ProjectPaths.chunkPath(id), buffer);

                        // returning record details to future.
                        return new ChunkRecords(id, position, size, crc.getValue());
                    }
                }));
            }
            // Collect results
            List<JsonObject> chunkJsonObjects = new ArrayList<>();
            for (Future<ChunkRecords> future : futures) {
                ChunkRecords chunk = future.get();
                JsonObject json = Json.createObjectBuilder()
                        .add("id", chunk.id)
                        .add("size", chunk.size)
                        .add("checksum", chunk.checksum)
                        .build();
                System.out.println(json);

                // Add to list for later file writing
                chunkJsonObjects.add(json);
            }

            // Printing into record file
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (JsonObject obj : chunkJsonObjects) {
                arrayBuilder.add(obj);
            }
            JsonArray jsonArray = arrayBuilder.build();

            // Pretty print configuration
            Map<String, Object> properties = new HashMap<>(1);
            properties.put(JsonGenerator.PRETTY_PRINTING, true);

            JsonWriterFactory writerFactory = Json.createWriterFactory(properties);

            try (FileWriter fileWriter = new FileWriter(ProjectPaths.MANIFEST_FILE.toFile());
                 JsonWriter jsonWriter = writerFactory.createWriter(fileWriter)) {
                jsonWriter.writeArray(jsonArray);
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }

        // printing the time taken by code
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Time taken: " + duration + " ms");
    }

}
