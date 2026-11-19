package no.nav.sikkerhetstjenesten.loggkamel.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

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
    void resultHasImmutableChronologicallyOrderedPaths() {
        List<Path> paths = new ArrayList<>(List.of(
                temp.resolve("202602-loggkamel-db.zip"), temp.resolve("202601-loggkamel-db.zip")));
        var archives = new TemporaryLocalArchive(temp, paths);
        paths.clear();

        assertEquals(List.of("202601-loggkamel-db.zip", "202602-loggkamel-db.zip"),
                archives.archivePaths().stream().map(path -> path.getFileName().toString()).toList());
        assertThrows(UnsupportedOperationException.class, () -> archives.archivePaths().clear());
    }

    @Test
    void failedDeletionWarnsWithoutThrowingAndStillAttemptsRemainingFiles() throws IOException {
        Path first = temp.resolve("202601-loggkamel-db.zip");
        Path second = temp.resolve("202602-loggkamel-db.zip");
        var archives = new TemporaryLocalArchive(temp, List.of(first, second));
        Logger logger = (Logger) LoggerFactory.getLogger(TemporaryLocalArchive.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
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
            assertTrue(appender.list.stream().anyMatch(event -> event.getLevel() == Level.WARN
                    && event.getFormattedMessage().contains("IOException")));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
