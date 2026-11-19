package no.nav.sikkerhetstjenesten.loggkamel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record TemporaryLocalArchive(Path temporaryDirectory, List<Path> archivePaths) implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TemporaryLocalArchive.class);

    public TemporaryLocalArchive {
        Objects.requireNonNull(temporaryDirectory, "temporaryDirectory");
        archivePaths = List.copyOf(archivePaths).stream()
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .toList();
    }

    @Override
    public void close() {
        try {
            if (Files.notExists(temporaryDirectory)) {
                return;
            }
            Files.walkFileTree(temporaryDirectory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException | RuntimeException exception) {
                        warnCleanupFailure(exception);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exception) {
                    warnCleanupFailure(exception);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException exception) {
                    if (exception != null) {
                        warnCleanupFailure(exception);
                    }
                    try {
                        Files.deleteIfExists(directory);
                    } catch (IOException | RuntimeException deleteException) {
                        warnCleanupFailure(deleteException);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException | RuntimeException exception) {
            warnCleanupFailure(exception);
        }
    }

    private void warnCleanupFailure(Exception exception) {
        log.warn("Could not fully clean temporary monthly cold-storage archive files, directory {} ({})",
                temporaryDirectory, exception.getClass().getSimpleName());
    }
}
