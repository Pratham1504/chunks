package chunks;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.CRC32;

public class ChunkThenCombine {
    //chunking
    private static String chunkBigFile(String FileName, int chunkSize) throws IOException {
        long size = Files.size(Path.of(FileName));

        try (FileChannel channel = FileChannel.open(Path.of(FileName), StandardOpenOption.READ)) {
            ForkJoinPool pool = new ForkJoinPool();
            ChunkRecursive mainChunk = new ChunkRecursive(0, 0, size, channel, chunkSize);
            List<ChunkRecords> records = pool.invoke(mainChunk);

            // Collect results
            List<JsonObject> chunkJsonObjects = new ArrayList<>();
            for (ChunkRecords record : records) {
                JsonObject json = Json.createObjectBuilder()
                        .add("id", record.id)
                        .add("size", record.size)
                        .add("checksum", record.checksum)
                        .build();
//                System.out.println(json);

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

            return ProjectPaths.MANIFEST_FILE.toString();
        }
    }


    private static String recombine(String recordFile, int chunkSize) throws IOException {
        List<ChunkRecordsNew> records = new ArrayList<>();
        int chunk_size = chunkSize;
        try (FileReader fileReader = new FileReader(recordFile);
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
                System.out.println("Recombining Complete!! recombined.bin file created!");
            } catch (IOException e){
                e.printStackTrace();
            }


        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
        return ProjectPaths.RECOMBINED_FILE.toString();
    }

    private static void verifyCombination(String FileName, String RecombinedFile)  {
        long checksum1 ;
        long checksum2 ;
        int size1;
        int size2;

        try (RandomAccessFile raf = new RandomAccessFile(FileName, "r")) {
            int size = Math.toIntExact(Files.size(Path.of(FileName)));
            size1 = size;
            byte[] buffer = new byte[size];
            raf.readFully(buffer);

            CRC32 crc = new CRC32();
            crc.update(buffer, 0, size);
            checksum1 = crc.getValue();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (RandomAccessFile raf = new RandomAccessFile(RecombinedFile, "r")) {
            int size = Math.toIntExact(Files.size(Path.of(RecombinedFile)));
            size2 = size;
            byte[] buffer = new byte[size];
            raf.readFully(buffer);

            CRC32 crc = new CRC32();
            crc.update(buffer, 0, size);
            checksum2 = crc.getValue();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(size2 != size1) {
            System.out.println(FileName + " & " + RecombinedFile + " files have Different size");
        } else {
            System.out.println(FileName + " & " + RecombinedFile + "files have same size");
        }

        if (checksum1 != checksum2) {
            System.out.println(FileName + " & " + RecombinedFile + " files have checksums don't match");
        } else  {
            System.out.println(FileName + " & " + RecombinedFile + " files have checksums match");
        }
    }

    public static void chunkThenCombine(String FileName, int chunkSize) throws IOException {

        ProjectPaths.ensureOutputDirectories();
        System.out.println("Chunking from " + FileName);
        String recordFile = chunkBigFile(FileName,chunkSize);

        System.out.println("Record file: " + recordFile);
        System.out.println("Staring Combining....");
        String recombinedFile = recombine(recordFile,chunkSize);
        System.out.println("Recombined File: " + recombinedFile);
        System.out.println("Finished Combining.... Now verifying...");
        verifyCombination(FileName, recombinedFile);
    }

    public static void main (String[] args) throws IOException {
        chunkThenCombine(ProjectPaths.INPUT_FILE.toString(), 1024 * 1024);
    }

}
