package no.nav.sikkerhetstjenesten.loggkamel.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public record ColdStorageArchive(
        Path archivePath,
        Path temporaryDirectory,
        String downloadFilename,
        long lineCount,
        int monthlyArchiveCount,
        long generationTimeMillis
) implements AutoCloseable {

    @Override
    public void close() throws IOException {
        try (var paths = Files.walk(temporaryDirectory)) {
            // Files.walk() returns a directory followed by its contents. Traversing it in reverse order lets us close the deeper
            // files first, so that attempting to close the higher-level directories can succeed (as they're empty when we try to close them)
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
