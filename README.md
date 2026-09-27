# File Chunking Examples

A set of Java examples for splitting a large file into fixed-size chunks, recording
checksums in a JSON manifest, rebuilding the original file, and verifying the result.

## Project layout

```text
src/main/java/chunks/       Chunking, recombination, verification, and file generation
src/main/java/chunks/oci/   OCI lifecycle-policy object lister
data/input/                 Generated input file (not tracked)
data/output/chunks/         Generated chunk files (not tracked)
data/output/manifests/      Generated JSON manifests (not tracked)
data/output/recombined.bin  Generated rebuilt file (not tracked)
data/archive/               Local test archives (not tracked)
```

All chunking programs use the paths defined in `ProjectPaths`, so generated files stay
out of the source and project root directories.

## Running the examples

Run from the project root.

1. Run `chunks.Main` to generate `data/input/bigfile.bin` (1 GB).
2. Run one of the chunkers, such as `chunks.Chunking` or `chunks.ChunkMultithreading`.
3. Run `chunks.Recombine` to rebuild the input file in `data/output/recombined.bin`.
4. Run `chunks.Verify` to compare the input and rebuilt file checksums.

`chunks.ChunkThenCombine` runs chunking, recombination, and verification together.

## Dependencies

The chunking classes that write or read manifests require JSON-P (`javax.json`, version
1.1.4 was used by the original project). The OCI object lister additionally requires the
Oracle Cloud Infrastructure Java SDK. Configure those dependencies in your IDE or build
tool before running the corresponding classes.

Generated binaries, manifests, archives, IDE settings, and build output are ignored by
Git; only source and documentation belong in the repository.
