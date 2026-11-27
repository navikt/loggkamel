package no.nav.sikkerhetstjenesten.loggkamel.service;

import com.google.api.gax.paging.Page;
import com.google.cloud.PageImpl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostgresInvalidMessageServiceTest {

    private static final String INVALID_BUCKET = "postgres-invalid";
    private static final String CONSUMER_BUCKET = "postgres-consumer";
    private static final String DBNAME = "example-db";

    @Test
    void streamLogFilesForTaskPropagatesListingFailure() {
        Storage storage = mock(Storage.class);
        IllegalStateException failure = new IllegalStateException("listing failed");
        when(storage.list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + "."))).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service(storage).streamLogFilesInBackoutQueueForTask(task())));
    }

    @Test
    void streamLogFilesForTaskReturnsEmptyStreamWhenNoObjectsMatch() {
        Storage storage = mock(Storage.class);
        when(storage.list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + "."))).thenReturn(page(List.of()));

        assertEquals(List.of(), service(storage).streamLogFilesInBackoutQueueForTask(task()).toList());
    }

    @Test
    void streamLogFilesForTaskExcludesOtherDatabasePrefixes() {
        Storage storage = mock(Storage.class);
        Blob matchingFile = blobNamed(DBNAME + ".20260101.auditlog");
        when(storage.list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + ".")))
                .thenReturn(page(List.of(matchingFile)));

        assertEquals(List.of(matchingFile), service(storage).streamLogFilesInBackoutQueueForTask(task()).toList());
        verify(storage).list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + "."));
    }

    @Test
    void streamLogFilesForTaskExcludesMalformedFilenames() {
        Storage storage = mock(Storage.class);
        Blob malformedDate = blobNamed(DBNAME + ".202601.auditlog");
        Blob malformedSuffix = blobNamed(DBNAME + ".20260101.auditlog.txt");
        Blob missingDate = blobNamed(DBNAME + ".auditlog");
        when(storage.list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + ".")))
                .thenReturn(page(List.of(malformedDate, malformedSuffix, missingDate)));

        assertEquals(List.of(), service(storage).streamLogFilesInBackoutQueueForTask(task()).toList());
    }

    @Test
    void streamLogFilesForTaskReadsAllListingPages() {
        Storage storage = mock(Storage.class);
        Blob firstFile = blobNamed(DBNAME + ".20260101.auditlog");
        Blob secondFile = blobNamed(DBNAME + ".20260102.auditlog.gz");
        Page<Blob> secondPage = page(List.of(secondFile));
        Page<Blob> firstPage = new PageImpl<>(() -> secondPage, "next-page", List.of(firstFile));
        when(storage.list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + "."))).thenReturn(firstPage);

        assertEquals(List.of(firstFile, secondFile),
                service(storage).streamLogFilesInBackoutQueueForTask(task()).toList());
    }

    @Test
    void moveLogsToConsumerBucketPropagatesInitialListingFailure() {
        Storage storage = mock(Storage.class);
        IllegalStateException failure = new IllegalStateException("listing failed");
        when(storage.list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + "."))).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service(storage).moveLogsToConsumerBucket(task())));
    }

    @Test
    void moveLogsToConsumerBucketCompletesWithoutCopyingWhenNoFilesMatch() {
        Storage storage = mock(Storage.class);
        when(storage.list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + "."))).thenReturn(page(List.of()));

        assertDoesNotThrow(() -> service(storage).moveLogsToConsumerBucket(task()));

        verify(storage, never()).copy(any(Storage.CopyRequest.class));
    }

    @Test
    void moveLogsToConsumerBucketDoesNotDeleteSourceWhenCopyStartFails() {
        Storage storage = mock(Storage.class);
        Blob source = source(DBNAME + ".20260101.auditlog");
        RuntimeException failure = new IllegalStateException("copy start failed");
        when(storage.list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + ".")))
                .thenReturn(page(List.of(source)));
        when(storage.copy(any(Storage.CopyRequest.class))).thenThrow(failure);

        assertSame(failure, assertThrows(RuntimeException.class,
                () -> service(storage).moveLogsToConsumerBucket(task())));

        verify(storage, never()).delete(anyString(), anyString(), any(Storage.BlobSourceOption[].class));
    }

    @Test
    void moveLogsToConsumerBucketDoesNotDeleteSourceWhenCopyCompletionFails() {
        Storage storage = mock(Storage.class);
        Blob source = source(DBNAME + ".20260101.auditlog");
        CopyWriter copyWriter = mock(CopyWriter.class);
        RuntimeException failure = new IllegalStateException("copy completion failed");
        when(storage.list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + ".")))
                .thenReturn(page(List.of(source)));
        when(storage.copy(any(Storage.CopyRequest.class))).thenReturn(copyWriter);
        when(copyWriter.getResult()).thenThrow(failure);

        assertSame(failure, assertThrows(RuntimeException.class,
                () -> service(storage).moveLogsToConsumerBucket(task())));

        verify(storage, never()).delete(anyString(), anyString(), any(Storage.BlobSourceOption[].class));
    }

    @Test
    void moveLogsToConsumerBucketPropagatesFailureWhileReadingLaterListingPage() {
        Storage storage = mock(Storage.class);
        RuntimeException failure = new IllegalStateException("later page failed");
        Page<Blob> firstPage = new PageImpl<>(() -> {
            throw failure;
        }, "next-page", List.of());
        when(storage.list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + "."))).thenReturn(firstPage);

        assertSame(failure, assertThrows(RuntimeException.class,
                () -> service(storage).moveLogsToConsumerBucket(task())));
    }

    @Test
    void moveLogsToConsumerBucketOverwritesExistingDestinationObject() {
        Storage storage = mock(Storage.class);
        String fileName = DBNAME + ".20260101.auditlog";
        Blob source = source(fileName);
        allowSuccessfulTransfer(storage, fileName);
        when(storage.list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + ".")))
                .thenReturn(page(List.of(source)));

        service(storage).moveLogsToConsumerBucket(task());

        ArgumentCaptor<Storage.CopyRequest> request = ArgumentCaptor.forClass(Storage.CopyRequest.class);
        verify(storage).copy(request.capture());
        assertEquals(CONSUMER_BUCKET, request.getValue().getTarget().getBucket());
        assertEquals(fileName, request.getValue().getTarget().getName());
        assertNull(request.getValue().getTarget().getGeneration());
        assertTrue(request.getValue().getTargetOptions().isEmpty());
    }

    @Test
    void moveLogsToConsumerBucketWaitsForCopyConfirmationBeforeDeletingSource() {
        Storage storage = mock(Storage.class);
        String fileName = DBNAME + ".20260101.auditlog";
        Blob source = source(fileName);
        CopyWriter copyWriter = mock(CopyWriter.class);
        when(storage.list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + ".")))
                .thenReturn(page(List.of(source)));
        when(storage.copy(any(Storage.CopyRequest.class))).thenReturn(copyWriter);
        when(storage.delete(eq(INVALID_BUCKET), eq(fileName), any(Storage.BlobSourceOption[].class)))
                .thenReturn(true);

        service(storage).moveLogsToConsumerBucket(task());

        InOrder order = inOrder(storage, copyWriter);
        order.verify(storage).copy(any(Storage.CopyRequest.class));
        order.verify(copyWriter).getResult();
        order.verify(storage).delete(eq(INVALID_BUCKET), eq(fileName), any(Storage.BlobSourceOption[].class));
    }

    @Test
    void moveLogsToConsumerBucketMovesAllMatchingFilesAcrossListingPages() {
        Storage storage = mock(Storage.class);
        String firstFilename = DBNAME + ".20260101.auditlog";
        Blob firstFile = source(firstFilename);
        String secondFilename = DBNAME + ".20260102.auditlog.gz";
        Blob secondFile = source(secondFilename);
        Page<Blob> secondPage = page(List.of(secondFile));
        Page<Blob> firstPage = new PageImpl<>(() -> secondPage, "next-page", List.of(firstFile));
        when(storage.list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + "."))).thenReturn(firstPage);
        allowSuccessfulCopy(storage);
        when(storage.delete(eq(INVALID_BUCKET), eq(firstFilename), any(Storage.BlobSourceOption[].class)))
                .thenReturn(true);
        when(storage.delete(eq(INVALID_BUCKET), eq(secondFilename), any(Storage.BlobSourceOption[].class)))
                .thenReturn(true);

        service(storage).moveLogsToConsumerBucket(task());

        verify(storage, org.mockito.Mockito.times(2)).copy(any(Storage.CopyRequest.class));
        verify(storage).delete(eq(INVALID_BUCKET), eq(firstFilename), any(Storage.BlobSourceOption[].class));
        verify(storage).delete(eq(INVALID_BUCKET), eq(secondFilename), any(Storage.BlobSourceOption[].class));
    }

    @Test
    void deleteTransferredSourcePropagatesDeletionErrorsOtherThanGenerationMismatch() {
        Storage storage = mock(Storage.class);
        String fileName = DBNAME + ".20260101.auditlog";
        Blob source = source(fileName);
        StorageException failure = new StorageException(500, "deletion failed");
        when(storage.list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + ".")))
                .thenReturn(page(List.of(source)));
        allowSuccessfulCopy(storage);
        when(storage.delete(eq(INVALID_BUCKET), eq(fileName), any(Storage.BlobSourceOption[].class)))
                .thenThrow(failure);

        assertSame(failure, assertThrows(StorageException.class,
                () -> service(storage).moveLogsToConsumerBucket(task())));
    }

    @Test
    void deleteTransferredSourceFailsWhenStorageDoesNotConfirmDeletion() {
        Storage storage = mock(Storage.class);
        String fileName = DBNAME + ".20260101.auditlog";
        Blob source = source(fileName);
        when(storage.list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + ".")))
                .thenReturn(page(List.of(source)));
        allowSuccessfulCopy(storage);
        when(storage.delete(eq(INVALID_BUCKET), eq(fileName), any(Storage.BlobSourceOption[].class)))
                .thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service(storage).moveLogsToConsumerBucket(task()));
    }

    @Test
    void deleteTransferredSourcePreservesReplacementGenerationWithoutFailingTransfer() {
        Storage storage = mock(Storage.class);
        String fileName = DBNAME + ".20260101.auditlog";
        Blob source = source(fileName);
        when(storage.list(INVALID_BUCKET, Storage.BlobListOption.prefix(DBNAME + ".")))
                .thenReturn(page(List.of(source)));
        allowSuccessfulCopy(storage);
        Storage.BlobSourceOption generationMatch = Storage.BlobSourceOption.generationMatch(1L);
        when(storage.delete(eq(INVALID_BUCKET), eq(fileName), eq(generationMatch)))
                .thenThrow(new StorageException(412, "generation changed"));

        assertDoesNotThrow(() -> service(storage).moveLogsToConsumerBucket(task()));

        verify(storage).delete(eq(INVALID_BUCKET), eq(fileName), eq(generationMatch));
        verify(storage, times(1)).delete(
                eq(INVALID_BUCKET), eq(fileName), any(Storage.BlobSourceOption[].class));
    }

    private static PostgresInvalidMessageService service(Storage storage) {
        return new PostgresInvalidMessageService(storage, INVALID_BUCKET, CONSUMER_BUCKET);
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

    private static void allowSuccessfulTransfer(Storage storage, String fileName) {
        allowSuccessfulCopy(storage);
        when(storage.delete(eq(INVALID_BUCKET), eq(fileName), any(Storage.BlobSourceOption[].class)))
                .thenReturn(true);
    }
}
