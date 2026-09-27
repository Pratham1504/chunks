package chunks;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.zip.CRC32;

public class ChunkRecursive extends RecursiveTask<List<ChunkRecords>> {
    private final int chunkId;
    private final long start,end;
    private final int chunkSize;
    private final FileChannel fileChannel;
    public ChunkRecursive(int chunkId, long start, long end, FileChannel fileChannel, int chunkSize) {
        this.chunkId = chunkId;
        this.start = start;
        this.end = end;
        this.fileChannel = fileChannel;
        this.chunkSize = chunkSize;
    }

    @Override
    protected List<ChunkRecords> compute() {
        try {
            long length = end - start;
            int totalChunks = (int) ((length + chunkSize - 1) / chunkSize);
//            System.out.println(chunkId + " " + start + " " + end + " " + totalChunks);
            if (totalChunks <= 2) {
//                System.out.println(chunkId );
                List<ChunkRecords> listResult = new ArrayList<>();
                for (int i = 0; i < totalChunks; i++) {
                    long pos = start + (long)(i * chunkSize);
                    int id = chunkId + i;

                    int size = (int) Math.min(chunkSize, length - (long)(i * chunkSize));
                    if (size <= 0) continue; // Don't process invalid chunks
//                    System.out.println(id);
                    ByteBuffer buffer = ByteBuffer.allocate(size);
                    fileChannel.read(buffer, pos);
                    byte[] bytes = buffer.array();
                    CRC32 crc = new CRC32();
                    crc.update(buffer.array(), 0, size);
                    long checkSum = crc.getValue();
//                    System.out.println(id);
                    Files.write(ProjectPaths.chunkPath(id), buffer.array());
                    listResult.add(new ChunkRecords(id, pos, size, checkSum));
                }
                return listResult;
            }
            //divide into left and right.
            long mid = start + length/ 2;
            int midId = chunkId + totalChunks / 2;
//            System.out.println(midId);
            ChunkRecursive left = new ChunkRecursive(chunkId, start, mid, fileChannel, chunkSize);
            ChunkRecursive right = new ChunkRecursive(midId, mid, end, fileChannel, chunkSize);
            left.fork();
            List<ChunkRecords> rightResult = right.compute();
            List<ChunkRecords> leftResult = left.join();
            List<ChunkRecords> finalResult = new ArrayList<>();
            finalResult.addAll(leftResult);
            finalResult.addAll(rightResult);
            return finalResult;

        }  catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
