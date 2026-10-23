package no.nav.sikkerhetstjenesten.loggkamel.service;

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
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DB2ColdStorageExportServiceTest {

    private static final String DB_NAME = "audit_db";
    private final AuditloggTaskService taskService = mock(AuditloggTaskService.class);
    private final LoggkamelProxyService proxyService = mock(LoggkamelProxyService.class);
    private final DB2ColdStorageExportService service = new DB2ColdStorageExportService(
            taskService, proxyService, new ObjectMapper().registerModule(new JavaTimeModule()),
            ZoneId.of("Europe/Oslo"));

    @Test
    void createExportBuildsMonthlyZipContainingUnmodifiedJsonAndCleansFilesOnClose() throws Exception {
        AuditloggTaskDTO task = task(Instant.parse("2026-02-01T23:30:00Z"));
        when(taskService.getAuditloggTaskByDbnameAndTeknologi(DB_NAME, TeknologiEnum.DB2)).thenReturn(task);
        DB2AuditloggLineDTO line = line(LocalDateTime.of(2026, Month.JANUARY, 31, 23, 59), "select * from audit");
        when(proxyService.getDB2AuditloggLinesForDatabaseInDateRange(
                eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(line));

        ColdStorageArchive archive = service.createExport(DB_NAME);
        Path archiveDirectory = archive.temporaryDirectory();
        try {
            assertEquals("audit_db.cold-storage.zip", archive.downloadFilename());
            assertEquals(1, archive.lineCount());
            assertEquals(1, archive.monthlyArchiveCount());
            verify(proxyService).getDB2AuditloggLinesForDatabaseInDateRange(
                    DB_NAME,
                    LocalDateTime.of(2026, 1, 1, 0, 0),
                    LocalDateTime.of(2026, 2, 2, 23, 59, 59, 999_999_999));

            List<ZipEntryData> outerEntries = readZip(Files.readAllBytes(archive.archivePath()));
            assertEquals(List.of("audit_db.01.2026.zip"), outerEntries.stream().map(ZipEntryData::name).toList());
            List<ZipEntryData> monthlyEntries = readZip(outerEntries.getFirst().contents());
            assertEquals(List.of("audit_db.01.2026.1.json"), monthlyEntries.stream().map(ZipEntryData::name).toList());
            String json = new String(monthlyEntries.getFirst().contents(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"databaseName\":\"audit_db\""));
            assertTrue(json.contains("\"sqlQuery\":\"select * from audit\""));
        } finally {
            archive.close();
        }
        assertFalse(Files.exists(archiveDirectory));
    }

    @Test
    void createExportRejectsTaskNotInColdStorageRequestedState() {
        AuditloggTaskDTO task = task(Instant.parse("2026-02-01T00:00:00Z"));
        when(task.getBackfill()).thenReturn(BackfillStatus.BACKFILL_REQUESTED);
        when(taskService.getAuditloggTaskByDbnameAndTeknologi(DB_NAME, TeknologiEnum.DB2)).thenReturn(task);

        assertThrows(InvalidAuditloggTaskStateException.class, () -> service.createExport(DB_NAME));
        verifyNoInteractions(proxyService);
    }

    @Test
    void createExportFailsAndCleansTemporaryFilesWhenProxyCannotAdvancePagination() throws Exception {
        AuditloggTaskDTO task = task(Instant.parse("2026-01-03T00:00:00Z"));
        when(taskService.getAuditloggTaskByDbnameAndTeknologi(DB_NAME, TeknologiEnum.DB2)).thenReturn(task);
        DB2AuditloggLineDTO repeatedLine = line(LocalDateTime.of(2026, 1, 2, 0, 0), "select 1");
        List<DB2AuditloggLineDTO> fullPage = new ArrayList<>();
        for (int i = 0; i < LoggkamelProxyService.MAX_DB2_PACKET_SIZE; i++) {
            fullPage.add(repeatedLine);
        }
        when(proxyService.getDB2AuditloggLinesForDatabaseInDateRange(
                eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(fullPage);

        long before = countColdStorageDirectories();
        assertThrows(ColdStoragePaginationException.class, () -> service.createExport(DB_NAME));
        assertEquals(before, countColdStorageDirectories());
    }

    @Test
    void createExportRemovesOverlappingLinesAndWritesOnlyMonthsWithLogs() throws Exception {
        AuditloggTaskDTO task = task(Instant.parse("2026-04-01T00:00:00Z"));
        when(taskService.getAuditloggTaskByDbnameAndTeknologi(DB_NAME, TeknologiEnum.DB2)).thenReturn(task);
        List<DB2AuditloggLineDTO> firstPage = new ArrayList<>();
        for (int i = 0; i < LoggkamelProxyService.MAX_DB2_PACKET_SIZE; i++) {
            firstPage.add(line(LocalDateTime.of(2026, 1, 2, 0, 0).plusNanos(i), "select " + i));
        }
        DB2AuditloggLineDTO lastLine = firstPage.getLast();
        DB2AuditloggLineDTO marchLine = line(LocalDateTime.of(2026, 3, 1, 0, 0), "select march");
        when(proxyService.getDB2AuditloggLinesForDatabaseInDateRange(
                eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(firstPage, List.of(lastLine, marchLine));

        ColdStorageArchive archive = service.createExport(DB_NAME);
        try {
            assertEquals(1_001, archive.lineCount());
            assertEquals(2, archive.monthlyArchiveCount());
            List<ZipEntryData> outerEntries = readZip(Files.readAllBytes(archive.archivePath()));
            assertEquals(List.of("audit_db.01.2026.zip", "audit_db.03.2026.zip"),
                    outerEntries.stream().map(ZipEntryData::name).toList());
            List<ZipEntryData> januaryPackets = readZip(outerEntries.getFirst().contents());
            assertEquals(List.of("audit_db.01.2026.1.json"), januaryPackets.stream().map(ZipEntryData::name).toList());
            assertEquals(1_000, new ObjectMapper().readTree(januaryPackets.getFirst().contents()).size());
            List<ZipEntryData> marchPackets = readZip(outerEntries.getLast().contents());
            assertEquals(List.of("audit_db.03.2026.1.json"), marchPackets.stream().map(ZipEntryData::name).toList());
            verify(proxyService, times(2)).getDB2AuditloggLinesForDatabaseInDateRange(
                    eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class));
        } finally {
            archive.close();
        }
    }

    @Test
    void createExportRejectsTaskWithoutUpdatedTimestamp() {
        AuditloggTaskDTO task = task(null);
        when(taskService.getAuditloggTaskByDbnameAndTeknologi(DB_NAME, TeknologiEnum.DB2)).thenReturn(task);

        assertThrows(InvalidAuditloggTaskStateException.class, () -> service.createExport(DB_NAME));
        verifyNoInteractions(proxyService);
    }

    private long countColdStorageDirectories() throws IOException {
        try (var paths = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return paths.filter(path -> path.getFileName().toString().startsWith("loggkamel-cold-storage-")).count();
        }
    }

    private static AuditloggTaskDTO task(Instant updated) {
        AuditloggTaskDTO task = mock(AuditloggTaskDTO.class);
        when(task.getDbname()).thenReturn(DB_NAME);
        when(task.getTeknologi()).thenReturn(TeknologiEnum.DB2);
        when(task.getBackfill()).thenReturn(BackfillStatus.COLD_STORAGE_REQUESTED);
        when(task.getUpdated()).thenReturn(updated);
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
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                ByteArrayOutputStream entryContents = new ByteArrayOutputStream();
                zipInput.transferTo(entryContents);
                entries.add(new ZipEntryData(entry.getName(), entryContents.toByteArray()));
            }
        }
        return entries;
    }

    private record ZipEntryData(String name, byte[] contents) {
    }
}
