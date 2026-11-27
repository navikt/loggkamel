package no.nav.sikkerhetstjenesten.loggkamel.service.archive;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskNotFoundException;
import no.nav.sikkerhetstjenesten.loggkamel.service.InvalidAuditloggTaskStateException;
import no.nav.sikkerhetstjenesten.loggkamel.service.PostgresInvalidMessageService;
import no.nav.sikkerhetstjenesten.loggkamel.service.TemporaryLocalArchive;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@ConditionalOnGCP
public class PostgresColdStorageArchiveService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String TEMP_DIRECTORY_PREFIX = "loggkamel-scheduled-cold-storage-";
    private final Storage storage;
    private final String sourceBucketName;
    private final PostgresInvalidMessageService invalidMessageService;

    public PostgresColdStorageArchiveService(
            @Qualifier("serviceLocalGcpStorage") Storage storage,
            @Value("${routing.postgres.invalid-message}") String sourceBucketName,
            PostgresInvalidMessageService invalidMessageService) {
        this.storage = storage;
        this.sourceBucketName = sourceBucketName;
        this.invalidMessageService = invalidMessageService;
    }

    public TemporaryLocalArchive createLocalArchiveOfColdStorageLogs(AuditloggTaskDTO task) {
        validateTaskIsPostgresAndRequestsColdStorage(task);
        Path temporaryDirectory;
        try {
            temporaryDirectory = Files.createTempDirectory(TEMP_DIRECTORY_PREFIX);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to prepare monthly cold-storage archives", exception);
        }

        List<Path> archivePaths = new ArrayList<>();
        // GCP guarantees ordering, we enforce it locally. Log files will be sorted from earliest to latest
        try (var sourceFiles = invalidMessageService.streamLogFilesInBackoutQueueForTask(task).sorted(Comparator.comparing(Blob::getName))) {
            var iterator = sourceFiles.iterator();
            Blob nextFile = iterator.hasNext() ? iterator.next() : null;
            // Outer loop iterates over each month with files, inner loop iterates over all files for the current month
            while (nextFile != null) {
                YearMonth month = monthOfFileDerivedFromFilename(nextFile);
                Path archivePath = temporaryDirectory.resolve(
                        month.format(MONTH_FORMAT) + "-loggkamel-" + task.getDbname() + ".zip");
                try (var zip = new ZipOutputStream(Files.newOutputStream(archivePath))) {
                    do {
                        zip.putNextEntry(new ZipEntry(nextFile.getName()));
                        try (var input = Channels.newInputStream(
                                storage.reader(sourceBucketName, nextFile.getName()))) {
                            input.transferTo(zip);
                        }
                        zip.closeEntry();
                        nextFile = iterator.hasNext() ? iterator.next() : null;
                    } while (nextFile != null && month.equals(monthOfFileDerivedFromFilename(nextFile)));
                }
                archivePaths.add(archivePath);
            }
            return new TemporaryLocalArchive(temporaryDirectory, archivePaths);
        } catch (IOException exception) {
            new TemporaryLocalArchive(temporaryDirectory, List.of()).close();
            throw new IllegalStateException("Unable to create monthly cold-storage archives", exception);
        } catch (RuntimeException exception) {
            new TemporaryLocalArchive(temporaryDirectory, List.of()).close();
            throw exception;
        }
    }

    public void deleteArchivedSourceFiles(AuditloggTaskDTO task) {
        validateTaskIsPostgresAndRequestsColdStorage(task);
        try (var sourceFiles = invalidMessageService.streamLogFilesInBackoutQueueForTask(task)) {
            var iterator = sourceFiles.iterator();
            while (iterator.hasNext()) {
                String filename = iterator.next().getName();
                storage.delete(sourceBucketName, filename);
            }
        }
    }

    private static YearMonth monthOfFileDerivedFromFilename(Blob file) {
        String filename = file.getName();
        int dateStart = filename.indexOf('.') + 1;
        return YearMonth.parse(filename.substring(dateStart, dateStart + 6), MONTH_FORMAT);
    }

    private static void validateTaskIsPostgresAndRequestsColdStorage(AuditloggTaskDTO task) {
        if (task == null) {
            throw new AuditloggTaskNotFoundException("No auditlogg task was provided for monthly cold storage");
        }
        if (task.getTeknologi() != TeknologiEnum.POSTGRESQL) {
            throw new InvalidAuditloggTaskStateException("Postgres cold-storage archives require a Postgres task");
        }
        if (task.getBackfill() != BackfillStatus.COLD_STORAGE_REQUESTED) {
            throw new InvalidAuditloggTaskStateException(
                    "Monthly cold-storage archives require cold-storage-requested task state");
        }
    }
}
