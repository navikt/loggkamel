package no.nav.sikkerhetstjenesten.loggkamel.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import no.nav.sikkerhetstjenesten.loggkamel.config.DB2ColdStorageProperties;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GCPColdStorageUploaderTest {

    private static final Path JANUARY = Path.of("202601-loggkamel-exampleDbName.zip");
    private static final Path FEBRUARY = Path.of("202602-loggkamel-exampleDbName.zip");
    private static final Path MARCH = Path.of("202603-loggkamel-exampleDbName.zip");

    @Mock
    Storage storage;

    GCPColdStorageUploader uploader;

    @BeforeEach
    void setUp() {
        uploader = new GCPColdStorageUploader(
                new DB2ColdStorageProperties("example-gcp-project", "example-bucket", "prod-gcp"), storage);
    }

    @Test
    void uploadsDirectlyToExactMonthlyPathsUsingCreateOnlyPrecondition() throws IOException {
        uploader.uploadLocalFilesToArchiveBucket(task("sikkerhetstjenesten"), List.of(JANUARY, FEBRUARY));

        ArgumentCaptor<BlobInfo> objects = ArgumentCaptor.forClass(BlobInfo.class);
        verify(storage).createFrom(objects.capture(), eq(JANUARY), eq(Storage.BlobWriteOption.doesNotExist()));
        verify(storage).createFrom(objects.capture(), eq(FEBRUARY), eq(Storage.BlobWriteOption.doesNotExist()));
        assertEquals(List.of(
                        "sikkerhetstjenesten-prod-gcp/" + JANUARY,
                        "sikkerhetstjenesten-prod-gcp/" + FEBRUARY),
                objects.getAllValues().stream().map(BlobInfo::getName).toList());
        assertTrue(objects.getAllValues().stream().allMatch(object ->
                object.getBucket().equals("example-bucket") && object.getContentType().equals("application/zip")));
        verifyNoMoreInteractions(storage);
    }

    @Test
    void preconditionFailureLeavesExistingObjectUntouchedAndContinues() throws IOException {
        when(storage.createFrom(any(BlobInfo.class), eq(JANUARY), any(Storage.BlobWriteOption.class)))
                .thenThrow(new StorageException(412, "conditionNotMet"));

        uploader.uploadLocalFilesToArchiveBucket(task("team"), List.of(JANUARY, FEBRUARY));

        verify(storage).createFrom(any(BlobInfo.class), eq(FEBRUARY), eq(Storage.BlobWriteOption.doesNotExist()));
        verifyNoMoreInteractions(storage);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 409, 429, 500, 503})
    void non412FailureStopsThisDatabaseBeforeRemainingMonths(int status) throws IOException {
        StorageException failure = new StorageException(status, "upload failed");
        when(storage.createFrom(any(BlobInfo.class), eq(JANUARY), any(Storage.BlobWriteOption.class)))
                .thenReturn(null);
        when(storage.createFrom(any(BlobInfo.class), eq(FEBRUARY), any(Storage.BlobWriteOption.class)))
                .thenThrow(failure);

        assertSame(failure, assertThrows(StorageException.class,
                () -> uploader.uploadLocalFilesToArchiveBucket(task("team"), List.of(JANUARY, FEBRUARY, MARCH))));

        verify(storage).createFrom(any(BlobInfo.class), eq(JANUARY), any(Storage.BlobWriteOption.class));
        verify(storage).createFrom(any(BlobInfo.class), eq(FEBRUARY), any(Storage.BlobWriteOption.class));
        verifyNoMoreInteractions(storage);
    }

    @Test
    void localReadFailureAlsoStopsRemainingMonths() throws IOException {
        when(storage.createFrom(any(BlobInfo.class), eq(JANUARY), any(Storage.BlobWriteOption.class)))
                .thenThrow(new IOException("read failed"));

        assertThrows(ColdStorageArchiveException.class,
                () -> uploader.uploadLocalFilesToArchiveBucket(task("team"), List.of(JANUARY, FEBRUARY)));

        verify(storage).createFrom(any(BlobInfo.class), eq(JANUARY), any(Storage.BlobWriteOption.class));
        verifyNoMoreInteractions(storage);
    }

    @Test
    void missingPermissionIsExplicitInWarnLogAndNotTreatedAsExistingObject() throws IOException {
        Logger logger = (Logger) LoggerFactory.getLogger(GCPColdStorageUploader.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            when(storage.createFrom(any(BlobInfo.class), eq(JANUARY), any(Storage.BlobWriteOption.class)))
                    .thenThrow(new StorageException(403, "Permission 'storage.objects.create' denied on resource"));

            assertThrows(StorageException.class, () -> uploader.uploadLocalFilesToArchiveBucket(task("team"), List.of(JANUARY, FEBRUARY)));

            assertTrue(appender.list.stream().anyMatch(event -> event.getLevel() == Level.WARN
                    && event.getFormattedMessage().contains("403")
                    && event.getFormattedMessage().contains("storage.objects.create")));
            verify(storage).createFrom(any(BlobInfo.class), eq(JANUARY), any(Storage.BlobWriteOption.class));
            verifyNoMoreInteractions(storage);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void retrySkipsExistingMonthsAndUploadsPreviouslyFailedMonth() throws IOException {
        when(storage.createFrom(any(BlobInfo.class), eq(JANUARY), any(Storage.BlobWriteOption.class)))
                .thenReturn(null)
                .thenThrow(new StorageException(412, "already exists"));
        when(storage.createFrom(any(BlobInfo.class), eq(FEBRUARY), any(Storage.BlobWriteOption.class)))
                .thenThrow(new StorageException(503, "unavailable"))
                .thenReturn(null);

        assertThrows(StorageException.class, () -> uploader.uploadLocalFilesToArchiveBucket(task("team"), List.of(JANUARY, FEBRUARY)));
        assertDoesNotThrow(() -> uploader.uploadLocalFilesToArchiveBucket(task("team"), List.of(JANUARY, FEBRUARY)));
        verify(storage, times(2)).createFrom(any(BlobInfo.class), eq(JANUARY), any(Storage.BlobWriteOption.class));
        verify(storage, times(2)).createFrom(any(BlobInfo.class), eq(FEBRUARY), any(Storage.BlobWriteOption.class));
        verifyNoMoreInteractions(storage);
    }

    @Test
    void emptyExportDoesNotUseStorageClient() {
        uploader.uploadLocalFilesToArchiveBucket(task("team"), List.of());
        verifyNoInteractions(storage);
    }

    @Test
    void placeholderNamesAndPathComponentsAreNotRejectedLocally() throws IOException {
        uploader = new GCPColdStorageUploader(
                new DB2ColdStorageProperties("CONFIGURED_DEV_PROJECT", "CONFIGURED_DEV_BUCKET", "custom/env"),
                storage);

        uploader.uploadLocalFilesToArchiveBucket(task("team/other"), List.of(JANUARY));

        var object = ArgumentCaptor.forClass(BlobInfo.class);
        verify(storage).createFrom(object.capture(), eq(JANUARY), eq(Storage.BlobWriteOption.doesNotExist()));
        assertEquals("CONFIGURED_DEV_BUCKET", object.getValue().getBucket());
        assertEquals("team/other-custom/env/" + JANUARY, object.getValue().getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void blankOwningTeamCannotCreateObjects(String team) {
        assertThrows(IllegalArgumentException.class, () -> uploader.uploadLocalFilesToArchiveBucket(task(team), List.of(JANUARY)));
        verifyNoInteractions(storage);
    }

    @Test
    void missingOwningTeamCannotCreateObjects() {
        assertThrows(IllegalArgumentException.class, () -> uploader.uploadLocalFilesToArchiveBucket(task(null), List.of(JANUARY)));
        verifyNoInteractions(storage);
    }

    private static AuditloggTaskDTO task(String team) {
        AuditloggTaskDTO task = mock(AuditloggTaskDTO.class);
        when(task.getNaisteam()).thenReturn(team);
        lenient().when(task.getDbname()).thenReturn("exampleDbName");
        return task;
    }
}
