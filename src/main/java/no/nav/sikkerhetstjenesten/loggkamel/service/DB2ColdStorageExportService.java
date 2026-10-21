package no.nav.sikkerhetstjenesten.loggkamel.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class DB2ColdStorageExportService {

    private static final Logger log = LoggerFactory.getLogger(DB2ColdStorageExportService.class);
    private static final LocalDate EXPORT_START_DATE = LocalDate.of(2026, 1, 1);
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MM.yyyy");

    private final AuditloggTaskService auditloggTaskService;
    private final LoggkamelProxyService loggkamelProxyService;
    private final ObjectMapper objectMapper;
    private final ZoneId appZoneId;

    public DB2ColdStorageExportService(
            AuditloggTaskService auditloggTaskService,
            LoggkamelProxyService loggkamelProxyService,
            ObjectMapper objectMapper,
            ZoneId appZoneId
    ) {
        this.auditloggTaskService = auditloggTaskService;
        this.loggkamelProxyService = loggkamelProxyService;
        this.objectMapper = objectMapper;
        this.appZoneId = appZoneId;
    }

    public ColdStorageArchive createExport(String dbName) {
        long startedAt = System.nanoTime();
        log.info("Starting DB2 cold-storage export for database {}", dbName);

        AuditloggTaskDTO task = auditloggTaskService.getAuditloggTaskByDbnameAndTeknologi(dbName, TeknologiEnum.DB2);
        assertDB2TaskExistsInValidState(dbName, task);

        String filenamePrefix = replaceNonASCIICharacters(dbName);
        Path temporaryDirectory;
        try {
            temporaryDirectory = Files.createTempDirectory("loggkamel-cold-storage-");
        } catch (IOException exception) {
            log.error("Could not create temporary storage for cold-storage export of database {} ({})",
                    dbName, exception.getClass().getSimpleName());
            throw new ColdStorageArchiveException("Unable to prepare the cold-storage archive", exception);
        }

        Instant updated = task.getUpdated();
        long lineCount = 0;
        Map<YearMonth, Path> monthlyArchives = new TreeMap<>();
        try {
            LocalDateTime rangeEnd = updated.atZone(appZoneId).toLocalDate().atTime(LocalTime.MAX);
            if (!rangeEnd.isBefore(EXPORT_START_DATE.atStartOfDay())) {
                try (MonthlyArchiveWriter writer = new MonthlyArchiveWriter(
                        temporaryDirectory, filenamePrefix, objectMapper, monthlyArchives)) {
                    lineCount = fetchAndWritePackets(
                            dbName,
                            EXPORT_START_DATE.atStartOfDay(),
                            rangeEnd,
                            writer);
                }
            }

            Path archivePath = temporaryDirectory.resolve(filenamePrefix + ".cold-storage.zip");
            writeOuterArchive(archivePath, monthlyArchives, filenamePrefix);
            long generationTimeMillis = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("Created DB2 cold-storage export for database {}: {} lines, {} monthly archives, {} ms",
                    dbName, lineCount, monthlyArchives.size(), generationTimeMillis);
            return new ColdStorageArchive(
                    archivePath,
                    temporaryDirectory,
                    filenamePrefix + ".cold-storage.zip",
                    lineCount,
                    monthlyArchives.size(),
                    generationTimeMillis);
        } catch (RuntimeException exception) {
            cleanupAfterFailure(temporaryDirectory, dbName, lineCount, exception);
            throw exception;
        } catch (IOException exception) {
            cleanupAfterFailure(temporaryDirectory, dbName, lineCount, exception);
            log.error("Could not create DB2 cold-storage archive for database {} after {} lines ({})",
                    dbName, lineCount, exception.getClass().getSimpleName());
            throw new ColdStorageArchiveException("Unable to create the cold-storage archive", exception);
        }
    }

    private void assertDB2TaskExistsInValidState(String dbName, AuditloggTaskDTO task) {
        if (task == null) {
            log.warn("Cold-storage export rejected because no DB2 task exists for database {}", dbName);
            throw new AuditloggTaskNotFoundException("No DB2 auditlogg task exists for database " + dbName);
        }
        if (task.getBackfill() != BackfillStatus.COLD_STORAGE_REQUESTED) {
            log.warn("Cold-storage export rejected because DB2 task for database {} is not in cold-storage-requested state", dbName);
            throw new InvalidAuditloggTaskStateException(
                    "DB2 task for database " + dbName + " is not in cold-storage-requested state");
        }
        Instant updated = task.getUpdated();
        if (updated == null) {
            log.warn("Cold-storage export rejected because DB2 task for database {} has no updated timestamp", dbName);
            throw new InvalidAuditloggTaskStateException(
                    "DB2 task for database " + dbName + " has no updated timestamp");
        }
    }

    private long fetchAndWritePackets(
            String dbName,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            MonthlyArchiveWriter writer
    ) throws IOException {
        long lineCount = 0;
        LocalDateTime dateTimeStartingNextPage = rangeStart;
        Set<DB2AuditloggLineDTO> seenInCurrentPageWithLatestTimestampOnPage = new HashSet<>();

        while (true) {
            List<DB2AuditloggLineDTO> loggLinesInCurrentPage;
            try {
                loggLinesInCurrentPage = loggkamelProxyService.getDB2AuditloggLinesForDatabaseInDateRange(dbName, dateTimeStartingNextPage, rangeEnd);
            } catch (RuntimeException exception) {
                throw new ColdStorageProxyException(exception);
            }
            if (loggLinesInCurrentPage.isEmpty()) {
                return lineCount;
            }

            LocalDateTime timestampOfLastLogInCurrentPage = loggLinesInCurrentPage.getLast().getMetricsTimestamp();

            LocalDateTime currentCursor = dateTimeStartingNextPage;
            Set<DB2AuditloggLineDTO> seenInLastPageWithTimestampOverlappingCurrentPageRange = seenInCurrentPageWithLatestTimestampOnPage;
            List<DB2AuditloggLineDTO> newLines = loggLinesInCurrentPage.stream()
                    .filter(line -> !line.getMetricsTimestamp().isEqual(currentCursor) || !seenInLastPageWithTimestampOverlappingCurrentPageRange.contains(line))
                    .toList();
            if (loggLinesInCurrentPage.size() >= MAX_DB2_PACKET_SIZE && newLines.isEmpty()) {
                throw new ColdStoragePaginationException(
                        "DB2 pagination did not advance because the result page repeated the same audit lines");
            }

            writer.writeLines(newLines);
            lineCount += newLines.size();

            if (timestampOfLastLogInCurrentPage.isEqual(dateTimeStartingNextPage)) {
                seenInCurrentPageWithLatestTimestampOnPage.addAll(loggLinesInCurrentPage);
            } else {
                dateTimeStartingNextPage = timestampOfLastLogInCurrentPage;
                seenInCurrentPageWithLatestTimestampOnPage = new HashSet<>();
                for (DB2AuditloggLineDTO line : loggLinesInCurrentPage) {
                    if (line.getMetricsTimestamp().isEqual(dateTimeStartingNextPage)) {
                        seenInCurrentPageWithLatestTimestampOnPage.add(line);
                    }
                }
            }

            if (loggLinesInCurrentPage.size() < MAX_DB2_PACKET_SIZE) {
                return lineCount;
            }
        }
    }

    private void writeOuterArchive(Path archivePath, Map<YearMonth, Path> monthlyArchives, String filenamePrefix)
            throws IOException {
        try (ZipOutputStream outerZip = new ZipOutputStream(Files.newOutputStream(archivePath))) {
            for (Map.Entry<YearMonth, Path> monthArchive : monthlyArchives.entrySet()) {
                String monthlyFilename = monthlyFilename(filenamePrefix, monthArchive.getKey());
                outerZip.putNextEntry(new ZipEntry(monthlyFilename));
                Files.copy(monthArchive.getValue(), outerZip);
                outerZip.closeEntry();
            }
        }
    }

    private static String replaceNonASCIICharacters(String dbName) {
        return dbName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String monthlyFilename(String filenamePrefix, YearMonth month) {
        return filenamePrefix + "." + month.format(MONTH_FORMAT) + ".zip";
    }

    private static void cleanupAfterFailure(Path temporaryDirectory, String dbName, long lineCount, Exception failure) {
        try (var paths = Files.walk(temporaryDirectory)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
            log.error("Could not clean temporary DB2 cold-storage files for database {} ({})",
                    dbName, cleanupFailure.getClass().getSimpleName());
        }
        log.error("DB2 cold-storage export failed for database {} after {} lines ({})",
                dbName, lineCount, failure.getClass().getSimpleName());
    }

    private static final class MonthlyArchiveWriter implements AutoCloseable {

        private final Path temporaryDirectory;
        private final String filenamePrefix;
        private final ObjectMapper objectMapper;
        private final Map<YearMonth, Path> monthlyArchives;
        private final Map<YearMonth, ZipOutputStream> outputStreams = new HashMap<>();
        private final Map<YearMonth, Integer> packetNumbers = new HashMap<>();

        private MonthlyArchiveWriter(
                Path temporaryDirectory,
                String filenamePrefix,
                ObjectMapper objectMapper,
                Map<YearMonth, Path> monthlyArchives
        ) {
            this.temporaryDirectory = temporaryDirectory;
            this.filenamePrefix = filenamePrefix;
            this.objectMapper = objectMapper;
            this.monthlyArchives = monthlyArchives;
        }

        private void writeLines(List<DB2AuditloggLineDTO> lines) throws IOException {
            Map<YearMonth, List<DB2AuditloggLineDTO>> linesByMonth = new TreeMap<>();
            for (DB2AuditloggLineDTO line : lines) {
                YearMonth month = YearMonth.from(line.getMetricsTimestamp());
                linesByMonth.computeIfAbsent(month, ignored -> new ArrayList<>()).add(line);
            }

            for (Map.Entry<YearMonth, List<DB2AuditloggLineDTO>> monthLines : linesByMonth.entrySet()) {
                YearMonth month = monthLines.getKey();
                ZipOutputStream monthZip = outputStreams.computeIfAbsent(month, this::openMonthlyArchive);
                int packetNumber = packetNumbers.merge(month, 1, Integer::sum);
                String packetFilename = filenamePrefix + "." + month.format(MONTH_FORMAT) + "." + packetNumber + ".json";
                monthZip.putNextEntry(new ZipEntry(packetFilename));
                objectMapper.writer()
                        .without(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                        .writeValue(monthZip, monthLines.getValue());
                monthZip.closeEntry();
            }
        }

        private ZipOutputStream openMonthlyArchive(YearMonth month) {
            Path path = temporaryDirectory.resolve("month-" + month + ".zip");
            try {
                ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(path));
                monthlyArchives.put(month, path);
                return zipOutputStream;
            } catch (IOException exception) {
                throw new ColdStorageArchiveException("Unable to create a monthly cold-storage archive", exception);
            }
        }

        @Override
        public void close() throws IOException {
            IOException closeFailure = null;
            for (ZipOutputStream outputStream : outputStreams.values()) {
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
