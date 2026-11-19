package no.nav.sikkerhetstjenesten.loggkamel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DB2ColdStorageArchiveServiceTest {

    private static final String DB_NAME = "audit_db";
    private final LoggkamelProxyService proxyService = mock(LoggkamelProxyService.class);
    private final DB2ColdStorageArchiveService service = new DB2ColdStorageArchiveService(
            proxyService,
            new ObjectMapper().registerModule(new JavaTimeModule()),
            ZoneId.of("Europe/Oslo"));

    @Test
    void createsChronologicalMonthlyArchivesContainingRawJsonPacketsAndUsesAppZoneEndDate() throws Exception {
        AuditloggTaskDTO task = task(Instant.parse("2026-02-01T23:30:00Z"), DB_NAME);
        DB2AuditloggLineDTO january = line(LocalDateTime.of(2026, 1, 31, 23, 59), "select january");
        DB2AuditloggLineDTO february = line(LocalDateTime.of(2026, 2, 2, 0, 0), "select february");
        when(proxyService.getDB2AuditloggLinesForDatabaseInDateRange(
                eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(january, february));

        try (TemporaryLocalArchive archives = service.createArchives(task)) {
            assertEquals(List.of(
                    "202601-loggkamel-audit_db.zip",
                    "202602-loggkamel-audit_db.zip"),
                    archives.archivePaths().stream().map(path -> path.getFileName().toString()).toList());
            verify(proxyService).getDB2AuditloggLinesForDatabaseInDateRange(
                    DB_NAME,
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2026, 2, 2, 23, 59, 59, 999_999_999));

            List<ZipEntryData> januaryEntries = readZip(Files.readAllBytes(archives.archivePaths().getFirst()));
            assertEquals(1, januaryEntries.size());
            JsonNode januaryJson = new ObjectMapper().readTree(januaryEntries.getFirst().contents());
            assertEquals(1, januaryJson.size());
            assertEquals("select january", januaryJson.get(0).get("sqlQuery").asText());
            assertEquals("audit_db", januaryJson.get(0).get("databaseName").asText());

            List<ZipEntryData> februaryEntries = readZip(Files.readAllBytes(archives.archivePaths().getLast()));
            assertEquals(1, februaryEntries.size());
            String json = new String(februaryEntries.getFirst().contents(), StandardCharsets.UTF_8);
            assertTrue(json.startsWith("["));
            assertTrue(json.contains("\"sqlQuery\":\"select february\""));
        }
    }

    @Test
    void supportsFixedStartDateAndArchivesThroughUpdatedDateIn2027() throws Exception {
        AuditloggTaskDTO task = task(Instant.parse("2026-12-31T23:30:00Z"), DB_NAME);
        when(proxyService.getDB2AuditloggLinesForDatabaseInDateRange(
                eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(line(LocalDateTime.of(2027, 1, 1, 0, 15), "select 2027")));

        try (TemporaryLocalArchive archives = service.createArchives(task)) {
            assertEquals(List.of("202701-loggkamel-audit_db.zip"),
                    archives.archivePaths().stream().map(path -> path.getFileName().toString()).toList());
            verify(proxyService).getDB2AuditloggLinesForDatabaseInDateRange(
                    DB_NAME,
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2027, 1, 1, 23, 59, 59, 999_999_999));
        }
    }

    @Test
    void writesOnlyMonthsWithLogsAndReturnsNoArchivesWhenThereAreNoLogs() throws Exception {
        AuditloggTaskDTO task = task(Instant.parse("2026-04-01T00:00:00Z"), DB_NAME);
        when(proxyService.getDB2AuditloggLinesForDatabaseInDateRange(
                eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        line(LocalDateTime.of(2026, 1, 2, 0, 0), "select january"),
                        line(LocalDateTime.of(2026, 3, 2, 0, 0), "select march")),
                        List.of());

        try (TemporaryLocalArchive archives = service.createArchives(task)) {
            assertEquals(List.of(
                    "202601-loggkamel-audit_db.zip",
                    "202603-loggkamel-audit_db.zip"),
                    archives.archivePaths().stream().map(path -> path.getFileName().toString()).toList());
        }

        when(proxyService.getDB2AuditloggLinesForDatabaseInDateRange(
                eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        try (TemporaryLocalArchive archives = service.createArchives(task)) {
            assertTrue(archives.archivePaths().isEmpty());
            assertThrows(UnsupportedOperationException.class, () -> archives.archivePaths().add(Path.of("x")));
        }
    }

    @Test
    void returnsSuccessfulEmptyArchivesWithoutFetchingWhenEndPrecedesFixedStart() throws Exception {
        AuditloggTaskDTO task = task(Instant.parse("2025-12-31T22:00:00Z"), DB_NAME);

        try (TemporaryLocalArchive archives = service.createArchives(task)) {
            assertTrue(archives.archivePaths().isEmpty());
            verifyNoInteractions(proxyService);
        }
    }

    @Test
    void removesOverlappingPaginationRowsAndWritesMultiplePages() throws Exception {
        AuditloggTaskDTO task = task(Instant.parse("2026-04-01T00:00:00Z"), DB_NAME);
        List<DB2AuditloggLineDTO> firstPage = new ArrayList<>();
        for (int i = 0; i < LoggkamelProxyService.MAX_DB2_PACKET_SIZE; i++) {
            firstPage.add(line(LocalDateTime.of(2026, 1, 2, 0, 0).plusNanos(i), "select " + i));
        }
        DB2AuditloggLineDTO overlapping = firstPage.getLast();
        DB2AuditloggLineDTO march = line(LocalDateTime.of(2026, 3, 1, 0, 0), "select march");
        when(proxyService.getDB2AuditloggLinesForDatabaseInDateRange(
                eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(firstPage, List.of(overlapping, march));

        try (TemporaryLocalArchive archives = service.createArchives(task)) {
            assertEquals(2, archives.archivePaths().size());
            List<ZipEntryData> januaryEntries = readZip(Files.readAllBytes(archives.archivePaths().getFirst()));
            assertEquals(1_000, new ObjectMapper().readTree(januaryEntries.getFirst().contents()).size());
            verify(proxyService, times(2)).getDB2AuditloggLinesForDatabaseInDateRange(
                    eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class));
        }
    }

    @Test
    void rejectsStalledPaginationAndCleansTemporaryFiles() throws Exception {
        AuditloggTaskDTO task = task(Instant.parse("2026-01-03T00:00:00Z"), DB_NAME);
        DB2AuditloggLineDTO repeated = line(LocalDateTime.of(2026, 1, 1, 0, 0), "select repeated");
        List<DB2AuditloggLineDTO> fullPage = new ArrayList<>();
        for (int i = 0; i < LoggkamelProxyService.MAX_DB2_PACKET_SIZE; i++) {
            fullPage.add(repeated);
        }
        when(proxyService.getDB2AuditloggLinesForDatabaseInDateRange(
                eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(fullPage);
        long before = countTemporaryDirectories();

        assertThrows(ColdStoragePaginationException.class, () -> service.createArchives(task));
        assertEquals(before, countTemporaryDirectories());
    }

    @Test
    void cleansCreatedArchivesWhenProxyFails() throws Exception {
        AuditloggTaskDTO task = task(Instant.parse("2026-04-01T00:00:00Z"), DB_NAME);
        List<DB2AuditloggLineDTO> firstPage = new ArrayList<>();
        for (int i = 0; i < LoggkamelProxyService.MAX_DB2_PACKET_SIZE; i++) {
            firstPage.add(line(LocalDateTime.of(2026, 1, 2, 0, 0).plusNanos(i), "select " + i));
        }
        when(proxyService.getDB2AuditloggLinesForDatabaseInDateRange(
                eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(firstPage)
                .thenThrow(new RuntimeException("proxy failure"));
        long before = countTemporaryDirectories();

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> service.createArchives(task));

        assertEquals("Unable to fetch DB2 audit logs for monthly cold storage", failure.getMessage());
        assertEquals(before, countTemporaryDirectories());
    }

    @Test
    void rejectsMissingUpdatedTimestampAndNonDb2OrWrongBackfillTasks() {
        assertThrows(InvalidAuditloggTaskStateException.class,
                () -> service.createArchives(task(null, DB_NAME)));
        assertThrows(InvalidAuditloggTaskStateException.class,
                () -> service.createArchives(task(
                        Instant.parse("2026-02-01T00:00:00Z"), DB_NAME, TeknologiEnum.POSTGRESQL,
                        BackfillStatus.COLD_STORAGE_REQUESTED)));
        assertThrows(InvalidAuditloggTaskStateException.class,
                () -> service.createArchives(task(
                        Instant.parse("2026-02-01T00:00:00Z"), DB_NAME, TeknologiEnum.DB2,
                        BackfillStatus.BACKFILL_REQUESTED)));
        verifyNoInteractions(proxyService);
    }

    private long countTemporaryDirectories() throws IOException {
        try (var paths = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return paths.filter(path -> path.getFileName().toString()
                    .startsWith("loggkamel-scheduled-cold-storage-")).count();
        }
    }

    private static AuditloggTaskDTO task(Instant updated, String dbName) {
        return task(updated, dbName, TeknologiEnum.DB2, BackfillStatus.COLD_STORAGE_REQUESTED);
    }

    private static AuditloggTaskDTO task(
            Instant updated,
            String dbName,
            TeknologiEnum technology,
            BackfillStatus backfill
    ) {
        AuditloggTaskDTO task = mock(AuditloggTaskDTO.class);
        when(task.getUpdated()).thenReturn(updated);
        when(task.getDbname()).thenReturn(dbName);
        when(task.getTeknologi()).thenReturn(technology);
        when(task.getBackfill()).thenReturn(backfill);
        return task;
    }

    private static DB2AuditloggLineDTO line(LocalDateTime timestamp, String sql) {
        return DB2AuditloggLineDTO.builder()
                .metricsTimestamp(timestamp)
                .databaseName(DB_NAME)
                .tableName("table_name")
                .authId("audit_user")
                .sqlQuery(sql)
                .build();
    }

    private static List<ZipEntryData> readZip(byte[] contents) throws IOException {
        List<ZipEntryData> entries = new ArrayList<>();
        try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(contents))) {
            var entry = zipInput.getNextEntry();
            while (entry != null) {
                ByteArrayOutputStream entryContents = new ByteArrayOutputStream();
                zipInput.transferTo(entryContents);
                entries.add(new ZipEntryData(entry.getName(), entryContents.toByteArray()));
                entry = zipInput.getNextEntry();
            }
        }
        return entries;
    }

    private record ZipEntryData(String name, byte[] contents) {
    }
}
