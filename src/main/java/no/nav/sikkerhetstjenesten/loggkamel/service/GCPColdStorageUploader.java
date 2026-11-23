package no.nav.sikkerhetstjenesten.loggkamel.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.config.ColdStorageProperties;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Component
@ConditionalOnGCP
public class GCPColdStorageUploader {

    private static final Logger log = LoggerFactory.getLogger(GCPColdStorageUploader.class);

    private final ColdStorageProperties properties;
    private final Storage storage;

    public GCPColdStorageUploader(
            ColdStorageProperties properties, @Lazy @Qualifier("coldStorage") Storage storage) {
        this.properties = properties;
        this.storage = storage;
    }

    public void uploadLocalFilesToArchiveBucket(AuditloggTaskDTO task, List<Path> archivePaths) {
        String owningTeam = task.getNaisteam();
        if (owningTeam == null || owningTeam.isBlank()) {
            throw new IllegalArgumentException("Cold-storage owning team must not be blank");
        }
        if (archivePaths.isEmpty()) {
            return;
        }

        String archiveBucketName = properties.bucketName();
        String teamDirectoryInArchiveBucket = owningTeam + "-" + properties.environment() + "/";
        for (Path archivePath : archivePaths) {
            String fileNameToUpload = teamDirectoryInArchiveBucket + archivePath.getFileName();
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(archiveBucketName, fileNameToUpload))
                    .setContentType("application/zip")
                    .build();
            try {
                storage.createFrom(blobInfo, archivePath, Storage.BlobWriteOption.doesNotExist());
                log.info("Uploaded cold-storage archive, database {}, bucket {}, object {}",
                        task.getDbname(), archiveBucketName, fileNameToUpload);
            } catch (StorageException exception) {
                if (exception.getCode() == 412) {
                    // Existing archives are deliberately accepted without read access or content verification.
                    log.info("Cold-storage archive already exists; leaving unchanged without verification, database {}, bucket {}, object {}",
                            task.getDbname(), archiveBucketName, fileNameToUpload);
                    continue;
                }
                log.warn("Cold-storage upload failed, database {}, project {}, bucket {}, object {}, status {}, reason {}, details {}",
                        task.getDbname(), properties.projectId(), archiveBucketName, fileNameToUpload,
                        exception.getCode(), exception.getReason(), exception.getMessage());
                throw exception;
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to read a monthly cold-storage archive for upload", exception);
            }
        }
    }

}
