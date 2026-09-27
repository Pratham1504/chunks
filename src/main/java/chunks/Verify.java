package chunks;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.CRC32;

public class Verify {
    public static void main(String[] args) throws IOException {
        long checksum1 ;
        long checksum2 ;
//        byte[] data1, data2;
        int size1;
        int size2;

        try (RandomAccessFile raf = new RandomAccessFile(ProjectPaths.INPUT_FILE.toFile(), "r")) {
            int size = Math.toIntExact(Files.size(ProjectPaths.INPUT_FILE));
            size1 = size;
            byte[] buffer = new byte[size];
//            raf.seek(0);
            raf.readFully(buffer);

            CRC32 crc = new CRC32();
            crc.update(buffer, 0, size);
            checksum1 = crc.getValue();
//            data1 =  buffer;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (RandomAccessFile raf = new RandomAccessFile(ProjectPaths.RECOMBINED_FILE.toFile(), "r")) {
            int size = Math.toIntExact(Files.size(ProjectPaths.RECOMBINED_FILE));
            size2 = size;
            byte[] buffer = new byte[size];
//            raf.seek(0);
            raf.readFully(buffer);

            CRC32 crc = new CRC32();
            crc.update(buffer, 0, size);
            checksum2 = crc.getValue();
//            data2 =  buffer;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(size2 != size1) {
            System.out.println("Different size");
        } else {
            System.out.println("Files have same size");
        }

        if (checksum1 != checksum2) {
            System.out.println("checksums don't match");
        } else  {
            System.out.println("checksums match");
        }


    }
}
