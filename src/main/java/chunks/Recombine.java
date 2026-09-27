package chunks;

import javax.json.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.CRC32;

class ChunkRecordsNew {

    public int id;
    public long position;
    public long size;
    public long checksum;
    public byte[] data;

    public ChunkRecordsNew() {
    }

    public ChunkRecordsNew(int id, long position, long size, long checksum, byte[] data) {
        this.id = id;
        this.position = position;
        this.size = size;
        this.checksum = checksum;
        this.data = data;
    }
}

public class Recombine {
    public static void main(String args[]) {
        //recording the start time.
        long startTime = System.currentTimeMillis();


        List<ChunkRecordsNew> records = new ArrayList<>();
        int chunk_size = 1024*1024;
        try (FileReader fileReader = new FileReader(ProjectPaths.MANIFEST_FILE.toFile());
             JsonReader jsonReader = Json.createReader(fileReader)) {

            // Parse the entire file as a JsonArray
            JsonArray jsonArray = jsonReader.readArray();

            // Iterate over the objects in the array and add to our list
            for (JsonValue value : jsonArray) {
                if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                    JsonObject obj = value.asJsonObject();
                    int id = obj.getInt("id");
                    long size = obj.getInt("size");
                    long checksum = obj.getJsonNumber("checksum").longValueExact();
//                    System.out.println("id: " + id + ", size: " + size + ", checksum: " + checksum);
                    byte[] data = null;
                    records.add(new ChunkRecordsNew(id, (long)id*chunk_size, size, checksum, data));
                }
            }
        } catch ( IOException e){
            e.printStackTrace();
        }

        //fixed thread pool of 5 threads
        ExecutorService executor = Executors.newFixedThreadPool(5);
        //futures will hold our record data.
        List<Future<ChunkRecordsNew>> futures = new ArrayList<>();

        try{
        int totalChunks = records.size();
        for (ChunkRecordsNew record: records) {
            futures.add(executor.submit(() -> {
                try (FileChannel channel = FileChannel.open(ProjectPaths.chunkPath(record.id), StandardOpenOption.READ)) {
                    ByteBuffer buffer = ByteBuffer.allocate((int)record.size);
                    channel.read(buffer);
                    byte[] bytes = buffer.array();

                    // checking the checksum
                    CRC32 crc = new CRC32();
                    crc.update(bytes, 0, (int) record.size);
                    if (crc.getValue() != record.checksum) {
                        System.out.println("Wrong checksum!");
                    }
                    // returning record details to future.
                    return new ChunkRecordsNew(record.id, record.position, record.size, record.checksum, bytes);
                }
            }));
        }

        List<ChunkRecordsNew> results = new ArrayList<>();
        for (Future<ChunkRecordsNew> future : futures) {
            ChunkRecordsNew chunk = future.get();
            results.add(chunk);
        }
        results.sort(Comparator.comparingInt(c -> c.id));
        try(OutputStream out = new BufferedOutputStream(new FileOutputStream(ProjectPaths.RECOMBINED_FILE.toFile()))) {
            for (ChunkRecordsNew record : results) {
                out.write(record.data);
            }
            System.out.println("Recombining Complete!!");
        } catch (IOException e){
            e.printStackTrace();
        }


    } catch ( InterruptedException | ExecutionException e) {
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
