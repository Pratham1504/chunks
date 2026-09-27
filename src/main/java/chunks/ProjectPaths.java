package chunks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Centralizes locations for generated input, chunk, manifest, and output files. */
final class ProjectPaths {
    static final Path INPUT_FILE = Path.of("data", "input", "bigfile.bin");
    static final Path CHUNKS_DIRECTORY = Path.of("data", "output", "chunks");
    static final Path MANIFEST_FILE = Path.of("data", "output", "manifests", "record-file.json");
    static final Path RECOMBINED_FILE = Path.of("data", "output", "recombined.bin");

    private ProjectPaths() {
    }

    static void ensureOutputDirectories() throws IOException {
        Files.createDirectories(INPUT_FILE.getParent());
        Files.createDirectories(CHUNKS_DIRECTORY);
        Files.createDirectories(MANIFEST_FILE.getParent());
        Files.createDirectories(RECOMBINED_FILE.getParent());
    }

    static Path chunkPath(int chunkId) {
        return CHUNKS_DIRECTORY.resolve("chunk_" + chunkId + ".bin");
    }
}
