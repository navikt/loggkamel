package no.nav.sikkerhetstjenesten.loggkamel.service;

import com.google.api.gax.paging.Page;
import com.google.cloud.PageImpl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgresInvalidMessageServiceTest {

    private static final String INVALID_MESSAGES_BUCKET = "postgres-invalid";
    private static final String CONSUMER_BUCKET = "postgres-consumer";
    private static final String DBNAME = "example-db";

    @Mock
    Storage storage;

    PostgresInvalidMessageService service;

    @BeforeEach
    void setUp() {
        service = new PostgresInvalidMessageService(storage, INVALID_MESSAGES_BUCKET, CONSUMER_BUCKET);
    }

    @Test
    void streamLogFilesForTaskPropagatesListingFailure() {
        IllegalStateException failure = new IllegalStateException("listing failed");
        when(storage.list(INVALID_MESSAGES_BUCKET, Storage.BlobListOption.prefix(DBNAME + "."))).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.streamLogFilesInBackoutQueueForTask(task())));
    }

    @Test
    void streamLogFilesForTaskReturnsEmptyStreamWhenNoObjectsMatch() {
        when(storage.list(INVALID_MESSAGES_BUCKET, Storage.BlobListOption.prefix(DBNAME + "."))).thenReturn(page(List.of()));

        assertEquals(List.of(), service.streamLogFilesInBackoutQueueForTask(task()).toList());
    }

    @Test
    void streamLogFilesForTaskExcludesMalformedFilenames() {
        Blob malformedDate = blobNamed(DBNAME + ".202601.auditlog");
        Blob malformedSuffix = blobNamed(DBNAME + ".20260101.auditlog.txt");
        Blob missingDate = blobNamed(DBNAME + ".auditlog");
        when(storage.list(INVALID_MESSAGES_BUCKET, Storage.BlobListOption.prefix(DBNAME + ".")))
                .thenReturn(page(List.of(malformedDate, malformedSuffix, missingDate)));

        assertEquals(List.of(), service.streamLogFilesInBackoutQueueForTask(task()).toList());
    }

    @Test
    void streamLogFilesForTaskReadsAllListingPages() {
        Blob firstFile = blobNamed(DBNAME + ".20260101.auditlog");
        Blob secondFile = blobNamed(DBNAME + ".20260102.auditlog.gz");
        Page<Blob> secondPage = page(List.of(secondFile));
        Page<Blob> firstPage = new PageImpl<>(() -> secondPage, "next-page", List.of(firstFile));
        when(storage.list(INVALID_MESSAGES_BUCKET, Storage.BlobListOption.prefix(DBNAME + "."))).thenReturn(firstPage);

        assertEquals(List.of(firstFile, secondFile),
                service.streamLogFilesInBackoutQueueForTask(task()).toList());
    }

    @Test
    void moveLogsToConsumerBucketPropagatesInitialListingFailure() {
        IllegalStateException failure = new IllegalStateException("listing failed");
        when(storage.list(INVALID_MESSAGES_BUCKET, Storage.BlobListOption.prefix(DBNAME + "."))).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.moveLogsToConsumerBucket(task())));
    }

    @Test
    void moveLogsToConsumerBucketCompletesWithoutCopyingWhenNoFilesMatch() {
        when(storage.list(INVALID_MESSAGES_BUCKET, Storage.BlobListOption.prefix(DBNAME + "."))).thenReturn(page(List.of()));

        assertDoesNotThrow(() -> service.moveLogsToConsumerBucket(task()));

        verify(storage, never()).copy(any(Storage.CopyRequest.class));
    }

    @Test
    void moveLogsToConsumerBucketDoesNotDeleteSourceWhenCopyStartFails() {
        Blob source = source(DBNAME + ".20260101.auditlog");
        RuntimeException failure = new IllegalStateException("copy start failed");
        when(storage.list(INVALID_MESSAGES_BUCKET, Storage.BlobListOption.prefix(DBNAME + ".")))
                .thenReturn(page(List.of(source)));
        when(storage.copy(any(Storage.CopyRequest.class))).thenThrow(failure);

        assertSame(failure, assertThrows(RuntimeException.class,
                () -> service.moveLogsToConsumerBucket(task())));

        verify(storage, never()).delete(anyString(), anyString(), any(Storage.BlobSourceOption[].class));
    }

    @Test
    void moveLogsToConsumerBucketDoesNotDeleteSourceWhenCopyCompletionFails() {
        Blob source = source(DBNAME + ".20260101.auditlog");
        CopyWriter copyWriter = mock(CopyWriter.class);
        RuntimeException failure = new IllegalStateException("copy completion failed");
        when(storage.list(INVALID_MESSAGES_BUCKET, Storage.BlobListOption.prefix(DBNAME + ".")))
                .thenReturn(page(List.of(source)));
        when(storage.copy(any(Storage.CopyRequest.class))).thenReturn(copyWriter);
        when(copyWriter.getResult()).thenThrow(failure);

        assertSame(failure, assertThrows(RuntimeException.class,
                () -> service.moveLogsToConsumerBucket(task())));

        verify(storage, never()).delete(anyString(), anyString(), any(Storage.BlobSourceOption[].class));
    }

    @Test
    void moveLogsToConsumerBucketWaitsForCopyConfirmationBeforeDeletingSource() {
        String fileName = DBNAME + ".20260101.auditlog";
        Blob source = source(fileName);
        CopyWriter copyWriter = mock(CopyWriter.class);
        when(storage.list(INVALID_MESSAGES_BUCKET, Storage.BlobListOption.prefix(DBNAME + ".")))
                .thenReturn(page(List.of(source)));
        when(storage.copy(any(Storage.CopyRequest.class))).thenReturn(copyWriter);
        when(storage.delete(eq(INVALID_MESSAGES_BUCKET), eq(fileName), any(Storage.BlobSourceOption[].class)))
                .thenReturn(true);

        service.moveLogsToConsumerBucket(task());

        InOrder order = inOrder(storage, copyWriter);
        order.verify(storage).copy(any(Storage.CopyRequest.class));
        order.verify(copyWriter).getResult();
        order.verify(storage).delete(eq(INVALID_MESSAGES_BUCKET), eq(fileName), any(Storage.BlobSourceOption[].class));
    }

    @Test
    void moveLogsToConsumerBucketMovesAllMatchingFilesAcrossListingPages() {
        String firstFilename = DBNAME + ".20260101.auditlog";
        Blob firstFile = source(firstFilename);
        String secondFilename = DBNAME + ".20260102.auditlog.gz";
        Blob secondFile = source(secondFilename);
        Page<Blob> secondPage = page(List.of(secondFile));
        Page<Blob> firstPage = new PageImpl<>(() -> secondPage, "next-page", List.of(firstFile));
        when(storage.list(INVALID_MESSAGES_BUCKET, Storage.BlobListOption.prefix(DBNAME + "."))).thenReturn(firstPage);
        allowSuccessfulCopy(storage);
        when(storage.delete(eq(INVALID_MESSAGES_BUCKET), eq(firstFilename), any(Storage.BlobSourceOption[].class)))
                .thenReturn(true);
        when(storage.delete(eq(INVALID_MESSAGES_BUCKET), eq(secondFilename), any(Storage.BlobSourceOption[].class)))
                .thenReturn(true);

        service.moveLogsToConsumerBucket(task());

        verify(storage, org.mockito.Mockito.times(2)).copy(any(Storage.CopyRequest.class));
        verify(storage).delete(eq(INVALID_MESSAGES_BUCKET), eq(firstFilename), any(Storage.BlobSourceOption[].class));
        verify(storage).delete(eq(INVALID_MESSAGES_BUCKET), eq(secondFilename), any(Storage.BlobSourceOption[].class));
    }

    @Test
    void deleteTransferredSourcePropagatesDeletionErrorsOtherThanGenerationMismatch() {
        String fileName = DBNAME + ".20260101.auditlog";
        Blob source = source(fileName);
        StorageException failure = new StorageException(500, "deletion failed");
        when(storage.list(INVALID_MESSAGES_BUCKET, Storage.BlobListOption.prefix(DBNAME + ".")))
                .thenReturn(page(List.of(source)));
        allowSuccessfulCopy(storage);
        when(storage.delete(eq(INVALID_MESSAGES_BUCKET), eq(fileName), any(Storage.BlobSourceOption[].class)))
                .thenThrow(failure);

        assertSame(failure, assertThrows(StorageException.class,
                () -> service.moveLogsToConsumerBucket(task())));
    }

    @Test
    void deleteTransferredSourceFailsWhenStorageDoesNotConfirmDeletion() {
        String fileName = DBNAME + ".20260101.auditlog";
        Blob source = source(fileName);
        when(storage.list(INVALID_MESSAGES_BUCKET, Storage.BlobListOption.prefix(DBNAME + ".")))
                .thenReturn(page(List.of(source)));
        allowSuccessfulCopy(storage);
        when(storage.delete(eq(INVALID_MESSAGES_BUCKET), eq(fileName), any(Storage.BlobSourceOption[].class)))
                .thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.moveLogsToConsumerBucket(task()));
    }

    @Test
    void deleteTransferredSourcePreservesReplacementGenerationWithoutFailingTransfer() {
        String fileName = DBNAME + ".20260101.auditlog";
        Blob source = source(fileName);
        when(storage.list(INVALID_MESSAGES_BUCKET, Storage.BlobListOption.prefix(DBNAME + ".")))
                .thenReturn(page(List.of(source)));
        allowSuccessfulCopy(storage);
        Storage.BlobSourceOption generationMatch = Storage.BlobSourceOption.generationMatch(1L);
        when(storage.delete(eq(INVALID_MESSAGES_BUCKET), eq(fileName), eq(generationMatch)))
                .thenThrow(new StorageException(412, "generation changed"));

        assertDoesNotThrow(() -> service.moveLogsToConsumerBucket(task()));

        verify(storage).delete(eq(INVALID_MESSAGES_BUCKET), eq(fileName), eq(generationMatch));
    }

    private static AuditloggTaskDTO task() {
        AuditloggTaskDTO task = mock(AuditloggTaskDTO.class);
        when(task.getDbname()).thenReturn(DBNAME);
        return task;
    }

    private static Blob source(String fileName) {
        Blob source = blobNamed(fileName);
        when(source.getGeneration()).thenReturn(1L);
        return source;
    }

    private static Blob blobNamed(String fileName) {
        Blob source = mock(Blob.class);
        when(source.getName()).thenReturn(fileName);
        return source;
    }

    private static Page<Blob> page(Iterable<Blob> values) {
        return new PageImpl<>(null, null, values);
    }

    private static void allowSuccessfulCopy(Storage storage) {
        CopyWriter copyWriter = mock(CopyWriter.class);
        when(storage.copy(any(Storage.CopyRequest.class))).thenReturn(copyWriter);
    }

}
