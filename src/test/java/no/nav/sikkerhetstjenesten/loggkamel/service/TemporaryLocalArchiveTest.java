package no.nav.sikkerhetstjenesten.loggkamel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TemporaryLocalArchiveTest {

    @TempDir Path temp;

    @Test
    void cleansWorkspaceAndIsSafeToCloseAgain() throws IOException {
        Path workspace = Files.createDirectory(temp.resolve("workspace"));
        Path archive = Files.createFile(workspace.resolve("202601-loggkamel-db.zip"));
        var archives = new TemporaryLocalArchive(workspace, List.of(archive));

        archives.close();

        assertFalse(Files.exists(workspace));
        assertDoesNotThrow(archives::close);
    }

    @Test
    void resultHasImmutablePathsInOriginalOrder() {
        List<Path> paths = new ArrayList<>(List.of(
                temp.resolve("202602-loggkamel-db.zip"), temp.resolve("202601-loggkamel-db.zip")));
        var archives = new TemporaryLocalArchive(temp, paths);
        paths.clear();

        assertEquals(List.of("202602-loggkamel-db.zip", "202601-loggkamel-db.zip"),
                archives.archivePaths().stream().map(path -> path.getFileName().toString()).toList());
        assertThrows(UnsupportedOperationException.class, () -> archives.archivePaths().clear());
    }

    @Test
    void failedDeletionDoesNotThrowAndStillAttemptsRemainingFiles() throws IOException {
        Path first = temp.resolve("202601-loggkamel-db.zip");
        Path second = temp.resolve("202602-loggkamel-db.zip");
        var archives = new TemporaryLocalArchive(temp, List.of(first, second));
        try (var files = mockStatic(Files.class)) {
            files.when(() -> Files.walkFileTree(eq(temp), any())).thenAnswer(invocation -> {
                FileVisitor<Path> visitor = invocation.getArgument(1);
                visitor.visitFile(first, null);
                visitor.visitFile(second, null);
                visitor.postVisitDirectory(temp, null);
                return temp;
            });
            files.when(() -> Files.deleteIfExists(first)).thenThrow(new IOException("disk failure"));

            assertDoesNotThrow(archives::close);

            files.verify(() -> Files.deleteIfExists(first));
            files.verify(() -> Files.deleteIfExists(second));
            files.verify(() -> Files.deleteIfExists(temp));
        }
    }
}
