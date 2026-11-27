package no.nav.sikkerhetstjenesten.loggkamel.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@ConditionalOnGCP
public class PostgresInvalidMessageService {

    private static final Logger log = LoggerFactory.getLogger(PostgresInvalidMessageService.class);

    // Example: example-db-name.20260327.auditlog.gz (or without .gz).
    private static final Pattern SOURCE_FILENAME_PATTERN =
            Pattern.compile("[^.]+\\.[0-9]{8}\\.auditlog(?:\\.gz)?");

    private final Storage storage;
    private final String postgresInvalidMessagesBucket;
    private final String postgresConsumerBucket;

    public PostgresInvalidMessageService(
            @Qualifier("serviceLocalGcpStorage") Storage storage,
            @Value("${routing.postgres.invalid-message}") String postgresInvalidMessagesBucket,
            @Value("${routing.postgres.consumer-bucket-name}") String postgresConsumerBucketName) {
        this.storage = storage;
        this.postgresInvalidMessagesBucket = postgresInvalidMessagesBucket;
        this.postgresConsumerBucket = postgresConsumerBucketName;
    }

    public Stream<Blob> streamLogFilesInBackoutQueueForTask(AuditloggTaskDTO task) {
        var files = storage.list(postgresInvalidMessagesBucket, Storage.BlobListOption.prefix(task.getDbname() + "."));
        return StreamSupport.stream(files.iterateAll().spliterator(), false)
                .filter(blob -> SOURCE_FILENAME_PATTERN.matcher(blob.getName()).matches());
    }

    public void moveLogsToConsumerBucket(AuditloggTaskDTO task) {
        try (var filesToMoveToConsumer = streamLogFilesInBackoutQueueForTask(task)) {
            var iterator = filesToMoveToConsumer.iterator();
            while (iterator.hasNext()) {
                Blob fileInBackoutQueue = iterator.next();
                storage.copy(Storage.CopyRequest.newBuilder()
                        .setSource(BlobId.of(postgresInvalidMessagesBucket, fileInBackoutQueue.getName(), fileInBackoutQueue.getGeneration()))
                        .setTarget(BlobId.of(postgresConsumerBucket, fileInBackoutQueue.getName()))
                        .build()).getResult();
                deleteTransferredSource(fileInBackoutQueue);
            }
        }
    }

    private void deleteTransferredSource(Blob source) {
        try {
            boolean deleted = storage.delete(postgresInvalidMessagesBucket, source.getName(),
                    Storage.BlobSourceOption.generationMatch(source.getGeneration()));
            if (!deleted) {
                throw new IllegalStateException("Transferred Postgres source file was not deleted: " + source.getName());
            }
        } catch (StorageException exception) {
            if (exception.getCode() != 412) {
                throw exception;
            }
            // A consumer backout can replace the source after the copy; that newer generation must stay.
            log.info("Transferred Postgres source {} was replaced; preserving the newer generation", source.getName());
        }
    }
}
