package no.nav.sikkerhetstjenesten.loggkamel.service.archive;

import com.google.api.gax.paging.Page;
import com.google.cloud.PageImpl;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskNotFoundException;
import no.nav.sikkerhetstjenesten.loggkamel.service.InvalidAuditloggTaskStateException;
import no.nav.sikkerhetstjenesten.loggkamel.service.PostgresInvalidMessageService;
import no.nav.sikkerhetstjenesten.loggkamel.service.TemporaryLocalArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostgresColdStorageArchiveServiceTest {

    private static final String DB_NAME = "example-db-name";
    private static final String ARCHIVE_BUCKET = "invalid-message-bucket";
    private static final String EARLY_JANUARY_LOGS_FILENAME = DB_NAME + ".20260102.auditlog";
    private static final String LATE_JANUARY_LOGS_FILENAME = DB_NAME + ".20260103.auditlog";
    private static final String FEBRUARY_LOGS_COMPRESSED_FILENAME = DB_NAME + ".20260201.auditlog.gz";
    private static final String TEMP_PREFIX = "loggkamel-scheduled-cold-storage-";

    @Mock
    Storage storage;

    @Mock
    AuditloggTaskDTO task;

    @TempDir
    Path temp;

    PostgresColdStorageArchiveService service;

    @BeforeEach
    void setUp() {
        service = new PostgresColdStorageArchiveService(storage, ARCHIVE_BUCKET,
                new PostgresInvalidMessageService(storage, ARCHIVE_BUCKET, "consumer-bucket"));
    }

    @Test
    void createLocalArchiveRejectsNullTask() {
        assertThrows(AuditloggTaskNotFoundException.class,
                () -> service.createLocalArchiveOfColdStorageLogs(null));
        verifyNoInteractions(storage);
    }

    @Test
    void createLocalArchiveRejectsNonPostgresTask() {
        when(task.getTeknologi()).thenReturn(TeknologiEnum.DB2);

        assertThrows(InvalidAuditloggTaskStateException.class,
                () -> service.createLocalArchiveOfColdStorageLogs(task));
        verifyNoInteractions(storage);
    }

    @Test
    void createLocalArchiveRejectsTaskOutsideColdStorageRequestedState() {
        when(task.getTeknologi()).thenReturn(TeknologiEnum.POSTGRESQL);
        when(task.getBackfill()).thenReturn(BackfillStatus.BACKFILL_REQUESTED);

        assertThrows(InvalidAuditloggTaskStateException.class,
                () -> service.createLocalArchiveOfColdStorageLogs(task));
        verifyNoInteractions(storage);
    }

    @Test
    void createLocalArchiveFailsWhenSourceListingFails() {
        stubTask();
        StorageException failure = new StorageException(503, "listing unavailable");
        when(storage.list(ARCHIVE_BUCKET, Storage.BlobListOption.prefix(DB_NAME + "."))).thenThrow(failure);

        assertSame(failure, assertThrows(StorageException.class,
                () -> service.createLocalArchiveOfColdStorageLogs(task)));
        verify(storage).list(ARCHIVE_BUCKET, Storage.BlobListOption.prefix(DB_NAME + "."));
        verifyNoMoreInteractions(storage);
    }

    @Test
    void createLocalArchiveCleansTemporaryDirectoryWhenCreationFails() throws IOException {
        stubTask();
        stubListing(page(EARLY_JANUARY_LOGS_FILENAME));
        IOException failure = new IOException("source read failed");
        ReadChannel reader = mock(ReadChannel.class);
        when(reader.read(any(ByteBuffer.class))).thenThrow(failure);
        when(storage.reader(ARCHIVE_BUCKET, EARLY_JANUARY_LOGS_FILENAME)).thenReturn(reader);
        Path directory = Files.createDirectory(temp.resolve("archive"));
        try (var files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            files.when(() -> Files.createTempDirectory(TEMP_PREFIX)).thenReturn(directory);

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> service.createLocalArchiveOfColdStorageLogs(task));

            assertSame(failure, exception.getCause());
            assertFalse(Files.exists(directory));
            verify(reader).close();
        }
    }

    @Test
    void createLocalArchiveHandlesEmptySourceSet() {
        stubTask();
        stubListing(page());

        try (TemporaryLocalArchive archive = service.createLocalArchiveOfColdStorageLogs(task)) {
            assertTrue(archive.archivePaths().isEmpty());
            verify(storage).list(ARCHIVE_BUCKET, Storage.BlobListOption.prefix(DB_NAME + "."));
            verifyNoMoreInteractions(storage);
        }
    }

    @Test
    void createLocalArchiveMatchesExactDatabasePrefix() {
        stubTask();
        stubListing(page(DB_NAME + ".other.20260102.auditlog"));

        try (TemporaryLocalArchive archive = service.createLocalArchiveOfColdStorageLogs(task)) {
            verify(storage).list(ARCHIVE_BUCKET, Storage.BlobListOption.prefix(DB_NAME + "."));
            assertTrue(archive.archivePaths().isEmpty());
            verifyNoMoreInteractions(storage);
        }
    }

    @Test
    void createLocalArchiveAcceptsPlainAuditlogSuffix() {
        assertAcceptedFilename(EARLY_JANUARY_LOGS_FILENAME);
    }

    @Test
    void createLocalArchiveAcceptsGzipAuditlogSuffix() {
        assertAcceptedFilename(FEBRUARY_LOGS_COMPRESSED_FILENAME);
    }

    @Test
    void createLocalArchiveReadsAllListingPages() throws IOException {
        stubTask();
        Page<Blob> second = page(LATE_JANUARY_LOGS_FILENAME);
        stubListing(new PageImpl<>(() -> second, "next", List.of(blob(EARLY_JANUARY_LOGS_FILENAME))));
        stubReader(EARLY_JANUARY_LOGS_FILENAME, new byte[0]);
        stubReader(LATE_JANUARY_LOGS_FILENAME, new byte[0]);

        try (TemporaryLocalArchive archive = service.createLocalArchiveOfColdStorageLogs(task);
             ZipFile zip = new ZipFile(archive.archivePaths().getFirst().toFile())) {
            assertEquals(List.of(EARLY_JANUARY_LOGS_FILENAME, LATE_JANUARY_LOGS_FILENAME), zip.stream().map(entry -> entry.getName()).toList());
        }
    }

    @Test
    void createLocalArchiveGroupsFilesByYearAndMonth() throws IOException {
        stubTask();
        String previousYearJanuaryFilename = DB_NAME + ".20250102.auditlog";
        stubListing(page(previousYearJanuaryFilename, EARLY_JANUARY_LOGS_FILENAME, LATE_JANUARY_LOGS_FILENAME, FEBRUARY_LOGS_COMPRESSED_FILENAME));
        for (String filename : List.of(previousYearJanuaryFilename, EARLY_JANUARY_LOGS_FILENAME, LATE_JANUARY_LOGS_FILENAME, FEBRUARY_LOGS_COMPRESSED_FILENAME)) {
            stubReader(filename, new byte[0]);
        }

        try (TemporaryLocalArchive archive = service.createLocalArchiveOfColdStorageLogs(task)) {
            assertEquals(List.of("202501-loggkamel-" + DB_NAME + ".zip",
                            "202601-loggkamel-" + DB_NAME + ".zip",
                            "202602-loggkamel-" + DB_NAME + ".zip"),
                    archive.archivePaths().stream().map(path -> path.getFileName().toString()).toList());
            try (ZipFile january2026ZipFile = new ZipFile(archive.archivePaths().get(1).toFile())) {
                assertEquals(List.of(EARLY_JANUARY_LOGS_FILENAME, LATE_JANUARY_LOGS_FILENAME),
                        january2026ZipFile.stream().map(entry -> entry.getName()).toList());
            }
        }
    }

    @Test
    void createLocalArchivePreservesSourceFilenamesInZip() throws IOException {
        stubTask();
        String earlyJanuaryCompressedLogsFilename = EARLY_JANUARY_LOGS_FILENAME + ".gz";
        stubListing(page(EARLY_JANUARY_LOGS_FILENAME, earlyJanuaryCompressedLogsFilename));
        stubReader(EARLY_JANUARY_LOGS_FILENAME, new byte[0]);
        stubReader(earlyJanuaryCompressedLogsFilename, new byte[0]);

        try (TemporaryLocalArchive archive = service.createLocalArchiveOfColdStorageLogs(task);
             ZipFile zip = new ZipFile(archive.archivePaths().getFirst().toFile())) {
            assertEquals(List.of(EARLY_JANUARY_LOGS_FILENAME, earlyJanuaryCompressedLogsFilename), zip.stream().map(entry -> entry.getName()).toList());
        }
    }

    @Test
    void createLocalArchivePreservesFileBytes() throws IOException {
        stubTask();
        byte[] original = {0, 1, 2, 10, 13, (byte) 255};
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(original);
        }
        stubListing(page(EARLY_JANUARY_LOGS_FILENAME, FEBRUARY_LOGS_COMPRESSED_FILENAME));
        stubReader(EARLY_JANUARY_LOGS_FILENAME, original);
        stubReader(FEBRUARY_LOGS_COMPRESSED_FILENAME, compressed.toByteArray());

        try (TemporaryLocalArchive archive = service.createLocalArchiveOfColdStorageLogs(task)) {
            assertEntryBytes(archive.archivePaths().getFirst(), EARLY_JANUARY_LOGS_FILENAME, original);
            assertEntryBytes(archive.archivePaths().getLast(), FEBRUARY_LOGS_COMPRESSED_FILENAME, compressed.toByteArray());
        }
    }

    @Test
    void deleteArchivedSourceFilesFailsWhenSourceListingFails() {
        stubTask();
        StorageException failure = new StorageException(503, "listing unavailable");
        when(storage.list(ARCHIVE_BUCKET, Storage.BlobListOption.prefix(DB_NAME + "."))).thenThrow(failure);

        assertSame(failure, assertThrows(StorageException.class, () -> service.deleteArchivedSourceFiles(task)));
        verify(storage).list(ARCHIVE_BUCKET, Storage.BlobListOption.prefix(DB_NAME + "."));
        verifyNoMoreInteractions(storage);
    }

    @Test
    void deleteArchivedSourceFilesStopsWhenDeletionFails() {
        stubTask();
        stubListing(page(EARLY_JANUARY_LOGS_FILENAME, FEBRUARY_LOGS_COMPRESSED_FILENAME));
        StorageException failure = new StorageException(503, "deletion unavailable");
        when(storage.delete(ARCHIVE_BUCKET, EARLY_JANUARY_LOGS_FILENAME)).thenThrow(failure);

        assertSame(failure, assertThrows(StorageException.class, () -> service.deleteArchivedSourceFiles(task)));
        verify(storage, never()).delete(ARCHIVE_BUCKET, FEBRUARY_LOGS_COMPRESSED_FILENAME);
    }

    @Test
    void deleteArchivedSourceFilesProcessesAllListingPages() {
        stubTask();
        Page<Blob> second = page(FEBRUARY_LOGS_COMPRESSED_FILENAME);
        stubListing(new PageImpl<>(() -> second, "next", List.of(blob(EARLY_JANUARY_LOGS_FILENAME))));

        service.deleteArchivedSourceFiles(task);

        verify(storage).delete(ARCHIVE_BUCKET, EARLY_JANUARY_LOGS_FILENAME);
        verify(storage).delete(ARCHIVE_BUCKET, FEBRUARY_LOGS_COMPRESSED_FILENAME);
    }

    private void stubTask() {
        when(task.getTeknologi()).thenReturn(TeknologiEnum.POSTGRESQL);
        when(task.getBackfill()).thenReturn(BackfillStatus.COLD_STORAGE_REQUESTED);
        when(task.getDbname()).thenReturn(DB_NAME);
    }

    private void stubListing(Page<Blob> page) {
        when(storage.list(ARCHIVE_BUCKET, Storage.BlobListOption.prefix(DB_NAME + "."))).thenReturn(page);
    }

    private static Page<Blob> page(String... filenames) {
        return new PageImpl<>(null, null, Arrays.stream(filenames)
                .map(PostgresColdStorageArchiveServiceTest::blob).toList());
    }

    private static Blob blob(String filename) {
        Blob blob = mock(Blob.class);
        lenient().when(blob.getName()).thenReturn(filename);
        return blob;
    }

    private void stubReader(String filename, byte[] bytes) throws IOException {
        var source = Channels.newChannel(new ByteArrayInputStream(bytes));
        ReadChannel reader = mock(ReadChannel.class);
        when(reader.read(any(ByteBuffer.class))).thenAnswer(invocation -> source.read(invocation.getArgument(0)));
        doAnswer(invocation -> {
            source.close();
            return null;
        }).when(reader).close();
        when(storage.reader(ARCHIVE_BUCKET, filename)).thenReturn(reader);
    }

    private void assertAcceptedFilename(String filename) {
        stubTask();
        stubListing(page(filename));
        StorageException stopAfterSelection = new StorageException(503, "stop at source read");
        when(storage.reader(ARCHIVE_BUCKET, filename)).thenThrow(stopAfterSelection);

        assertSame(stopAfterSelection, assertThrows(StorageException.class,
                () -> service.createLocalArchiveOfColdStorageLogs(task)));
        verify(storage).reader(ARCHIVE_BUCKET, filename);
    }

    private static void assertEntryBytes(Path archive, String filename, byte[] expected) throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile());
             var input = zip.getInputStream(zip.getEntry(filename))) {
            assertArrayEquals(expected, input.readAllBytes());
        }
    }
}
