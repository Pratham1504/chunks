package chunks;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;



public class Chunking {
    public static void main(String[] args) throws IOException {
        //recording the start time.
        long startTime = System.currentTimeMillis();

        ProjectPaths.ensureOutputDirectories();
        long size = Files.size(ProjectPaths.INPUT_FILE);

        try (FileChannel channel = FileChannel.open(ProjectPaths.INPUT_FILE, StandardOpenOption.READ)) {
            ForkJoinPool pool = new ForkJoinPool();
            ChunkRecursive mainChunk = new ChunkRecursive(0, 0, size, channel, 1024*1024);
            List<ChunkRecords> records = pool.invoke(mainChunk);

            // Collect results
            List<JsonObject> chunkJsonObjects = new ArrayList<>();
            for (ChunkRecords record : records) {
                JsonObject json = Json.createObjectBuilder()
                        .add("id", record.id)
                        .add("size", record.size)
                        .add("checksum", record.checksum)
                        .build();
                System.out.println(json);

                // Add to list for later file writing
                chunkJsonObjects.add(json);
            }

            // Printing into record file
            JsonArrayBuilder arrayBuilder2 = Json.createArrayBuilder();
            for (JsonObject obj : chunkJsonObjects) {
                arrayBuilder2.add(obj);
            }
            JsonArray jsonArray = arrayBuilder2.build();

            // Pretty print configuration
            Map<String, Object> properties = new HashMap<>(1);
            properties.put(JsonGenerator.PRETTY_PRINTING, true);

            JsonWriterFactory writerFactory = Json.createWriterFactory(properties);

            try (FileWriter fileWriter = new FileWriter(ProjectPaths.MANIFEST_FILE.toFile());
                 JsonWriter jsonWriter = writerFactory.createWriter(fileWriter)) {
                jsonWriter.writeArray(jsonArray);
            }
            pool.shutdown();

        }

        // printing the time taken by code
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Time taken: " + duration + " ms");

    }
}
