package no.nav.sikkerhetstjenesten.loggkamel.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static no.nav.sikkerhetstjenesten.loggkamel.service.LoggkamelProxyService.MAX_DB2_PACKET_SIZE;

@Service
public class DB2ColdStorageArchiveService {

    private static final LocalDate COLD_STORAGE_PERIOD_START_DATE = LocalDate.of(2026, 1, 1);
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String TEMP_DIRECTORY_PREFIX = "loggkamel-scheduled-cold-storage-";

    private final LoggkamelProxyService loggkamelProxyService;
    private final ObjectMapper objectMapper;
    private final ZoneId appZoneId;

    public DB2ColdStorageArchiveService(
            LoggkamelProxyService loggkamelProxyService,
            ObjectMapper objectMapper,
            ZoneId appZoneId
    ) {
        this.loggkamelProxyService = loggkamelProxyService;
        this.objectMapper = objectMapper;
        this.appZoneId = appZoneId;
    }

    public TemporaryLocalArchive createArchives(AuditloggTaskDTO task) {
        validateTaskIsDB2AndRequestsColdStorage(task);
        String dbName = task.getDbname();

        Path tempLocalArchiveStorageDirectory;
        try {
            tempLocalArchiveStorageDirectory = Files.createTempDirectory(TEMP_DIRECTORY_PREFIX);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to prepare monthly cold-storage archives", exception);
        }

        Map<YearMonth, Path> archivesByMonth = new TreeMap<>();
        try {
            Instant updated = task.getUpdated();
            LocalDateTime coldStoragePeriodEndDate = updated.atZone(appZoneId).toLocalDate().atTime(LocalTime.MAX);
            if (!coldStoragePeriodEndDate.isBefore(COLD_STORAGE_PERIOD_START_DATE.atStartOfDay())) {
                try (MonthlyArchiveWriter writer = new MonthlyArchiveWriter(
                        tempLocalArchiveStorageDirectory, dbName, objectMapper, archivesByMonth)) {
                    fetchAndWritePacketsInDateRangeAsMonthlyZipFiles(dbName, COLD_STORAGE_PERIOD_START_DATE.atStartOfDay(), coldStoragePeriodEndDate, writer);
                }
            }
            return new TemporaryLocalArchive(tempLocalArchiveStorageDirectory, new ArrayList<>(archivesByMonth.values()));
        } catch (IOException exception) {
            cleanupAfterFailure(tempLocalArchiveStorageDirectory);
            throw new IllegalStateException("Unable to create monthly cold-storage archives", exception);
        } catch (RuntimeException exception) {
            cleanupAfterFailure(tempLocalArchiveStorageDirectory);
            throw exception;
        }
    }

    private static void validateTaskIsDB2AndRequestsColdStorage(AuditloggTaskDTO task) {
        if (task == null) {
            throw new AuditloggTaskNotFoundException("No auditlogg task was provided for monthly cold storage");
        }
        if (task.getUpdated() == null) {
            throw new InvalidAuditloggTaskStateException("Auditlogg task has no updated timestamp");
        }
        if (task.getTeknologi() != TeknologiEnum.DB2) {
            throw new InvalidAuditloggTaskStateException("Monthly cold-storage archives require a DB2 task");
        }
        if (task.getBackfill() != BackfillStatus.COLD_STORAGE_REQUESTED) {
            throw new InvalidAuditloggTaskStateException(
                    "Monthly cold-storage archives require cold-storage-requested task state");
        }
    }

    private void fetchAndWritePacketsInDateRangeAsMonthlyZipFiles(
            String dbName,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            MonthlyArchiveWriter writer
    ) throws IOException {
        LocalDateTime firstTimestampInCurrentPage = rangeStart;
        Set<DB2AuditloggLineDTO> logLinesSeenInPreviousPage = new HashSet<>();

        while (true) {
            List<DB2AuditloggLineDTO> page;
            try {
                page = loggkamelProxyService.getDB2AuditloggLinesForDatabaseInDateRange(dbName, firstTimestampInCurrentPage, rangeEnd);
            } catch (RuntimeException exception) {
                throw new IllegalStateException("Unable to fetch DB2 audit logs for monthly cold storage", exception);
            }
            if (page.isEmpty()) {
                return;
            }

            LocalDateTime lastTimestampInCurrentPage = page.getLast().getMetricsTimestamp();
            LocalDateTime currentCursor = firstTimestampInCurrentPage;
            List<DB2AuditloggLineDTO> newLines = page.stream()
                    .filter(line -> !line.getMetricsTimestamp().isEqual(currentCursor) || !logLinesSeenInPreviousPage.contains(line))
                    .toList();
            if (page.size() >= MAX_DB2_PACKET_SIZE && lastTimestampInCurrentPage.isEqual(firstTimestampInCurrentPage) && newLines.isEmpty()) {
                throw new ColdStoragePaginationException(
                        "DB2 pagination did not advance because the result page repeated the same audit lines");
            }

            writer.writeLines(newLines);

            firstTimestampInCurrentPage = lastTimestampInCurrentPage;
            logLinesSeenInPreviousPage.clear();
            for (DB2AuditloggLineDTO line : page) {
                if (line.getMetricsTimestamp().isEqual(firstTimestampInCurrentPage)) {
                    logLinesSeenInPreviousPage.add(line);
                }
            }

            // If loggkamel-proxy returned a packet under the maximum packet size (before we filter already-seen lines)
            // then there are no more logs in this range
            if (page.size() < MAX_DB2_PACKET_SIZE) {
                return;
            }
        }
    }

    private static void cleanupAfterFailure(Path temporaryDirectory) {
        new TemporaryLocalArchive(temporaryDirectory, List.of()).close();
    }

    private static final class MonthlyArchiveWriter implements AutoCloseable {

        private final Path temporaryDirectory;
        private final String dbName;
        private final ObjectMapper objectMapper;
        private final Map<YearMonth, Path> archivePathsForEachMonth;
        private final Map<YearMonth, ZipOutputStream> outputZipStreamsByMonth = new HashMap<>();
        private final Map<YearMonth, Integer> packetsProcessedPerMonth = new HashMap<>();

        private MonthlyArchiveWriter(
                Path temporaryDirectory,
                String dbName,
                ObjectMapper objectMapper,
                Map<YearMonth, Path> archivePathsForEachMonth
        ) {
            this.temporaryDirectory = temporaryDirectory;
            this.dbName = dbName;
            this.objectMapper = objectMapper;
            this.archivePathsForEachMonth = archivePathsForEachMonth;
        }

        private void writeLines(List<DB2AuditloggLineDTO> lines) throws IOException {
            Map<YearMonth, List<DB2AuditloggLineDTO>> linesByMonth = new TreeMap<>();
            for (DB2AuditloggLineDTO line : lines) {
                YearMonth month = YearMonth.from(line.getMetricsTimestamp());
                linesByMonth.computeIfAbsent(month, ignored -> new ArrayList<>()).add(line);
            }

            for (Map.Entry<YearMonth, List<DB2AuditloggLineDTO>> monthAndAssociatedLines : linesByMonth.entrySet()) {
                YearMonth monthBeingProcessed = monthAndAssociatedLines.getKey();
                ZipOutputStream zipFileStreamForMonthBeingProcessed = outputZipStreamsByMonth.get(monthBeingProcessed);
                if (zipFileStreamForMonthBeingProcessed == null) {
                    Path archivePath = temporaryDirectory.resolve(monthBeingProcessed.format(MONTH_FORMAT)
                            + "-loggkamel-" + dbName + ".zip");
                    zipFileStreamForMonthBeingProcessed = new ZipOutputStream(Files.newOutputStream(archivePath));
                    outputZipStreamsByMonth.put(monthBeingProcessed, zipFileStreamForMonthBeingProcessed);
                    archivePathsForEachMonth.put(monthBeingProcessed, archivePath);
                }

                int packetNumber = packetsProcessedPerMonth.merge(monthBeingProcessed, 1, Integer::sum);
                String packetFilename = monthBeingProcessed.format(MONTH_FORMAT) + "-loggkamel-" + dbName
                        + "-" + packetNumber + ".json";
                zipFileStreamForMonthBeingProcessed.putNextEntry(new ZipEntry(packetFilename));
                objectMapper.writer()
                        .without(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                        .writeValue(zipFileStreamForMonthBeingProcessed, monthAndAssociatedLines.getValue());
                zipFileStreamForMonthBeingProcessed.closeEntry();
            }
        }

        @Override
        public void close() throws IOException {
            IOException closeFailure = null;
            for (ZipOutputStream outputStream : outputZipStreamsByMonth.values()) {
                try {
                    outputStream.close();
                } catch (IOException exception) {
                    if (closeFailure == null) {
                        closeFailure = exception;
                    } else {
                        closeFailure.addSuppressed(exception);
                    }
                }
            }
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }
}
