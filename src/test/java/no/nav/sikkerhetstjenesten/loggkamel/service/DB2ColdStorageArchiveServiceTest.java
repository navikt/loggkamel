package no.nav.sikkerhetstjenesten.loggkamel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.zip.ZipInputStream;

import static java.util.Collections.nCopies;
import static no.nav.sikkerhetstjenesten.loggkamel.service.LoggkamelProxyService.MAX_DB2_PACKET_SIZE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DB2ColdStorageArchiveServiceTest {

    private static final String DB_NAME = "audit_db";
    private static final String SQL_STATEMENT_1 = "sql statement 1";
    private static final String SQL_STATEMENT_2 = "sql statement 2";
    private static final String TEMP_DIRECTORY_PREFIX = "loggkamel-scheduled-cold-storage-";
    private static final Instant UPDATED_IN_APRIL = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant UPDATED_BEFORE_START = Instant.parse("2025-12-31T22:00:00Z");
    private static final Instant UPDATED_AT_UTC_DAY_END = Instant.parse("2026-02-01T23:30:00Z");
    private static final LocalDateTime PERIOD_START = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDateTime JANUARY_TIMESTAMP = LocalDateTime.of(2026, 1, 2, 0, 0);
    private static final LocalDateTime MARCH_TIMESTAMP = LocalDateTime.of(2026, 3, 1, 0, 0);
    private static final Path JANUARY_ARCHIVE_PATH = Path.of("202601-loggkamel-audit_db.zip");
    private static final Path MARCH_ARCHIVE_PATH = Path.of("202603-loggkamel-audit_db.zip");

    @Mock
    LoggkamelProxyService proxyService;

    @Mock
    AuditloggTaskDTO auditloggTaskDTO;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private DB2ColdStorageArchiveService service;

    @BeforeEach
    void setUp() {
        service = new DB2ColdStorageArchiveService(proxyService, objectMapper, ZoneId.of("Europe/Oslo"));
    }

    @Test
    void createsChronologicalMonthlyArchivesContainingRawJsonPacketsAndUsesAppZoneEndDate() throws IOException {
        stubColdStorageTask(UPDATED_AT_UTC_DAY_END);
        DB2AuditloggLineDTO januaryLoggLine = line(JANUARY_TIMESTAMP, SQL_STATEMENT_1);
        DB2AuditloggLineDTO februaryLoggLine = line(JANUARY_TIMESTAMP.plusMonths(1), SQL_STATEMENT_2);
        when(proxyService.getDB2AuditloggLinesForDatabaseInDateRange(
                eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(januaryLoggLine, februaryLoggLine));

        try (TemporaryLocalArchive archives = service.createLocalArchiveOfColdStorageLogs(auditloggTaskDTO)) {
            assertEquals(List.of(JANUARY_ARCHIVE_PATH, Path.of("202602-loggkamel-audit_db.zip")),
                    archives.archivePaths().stream().map(Path::getFileName).toList());
            verify(proxyService).getDB2AuditloggLinesForDatabaseInDateRange(
                    DB_NAME,
                    PERIOD_START,
                    LocalDateTime.of(2026, 2, 2, 23, 59, 59, 999_999_999));

            List<JsonNode> januaryPackets = readPackets(archives.archivePaths().get(0));
            assertEquals(1, januaryPackets.size());
            JsonNode januaryJson = januaryPackets.getFirst();
            assertTrue(januaryJson.isArray());
            assertEquals(1, januaryJson.size());
            assertEquals(SQL_STATEMENT_1, januaryJson.get(0).get("sqlQuery").asText());
            assertEquals(DB_NAME, januaryJson.get(0).get("databaseName").asText());

            List<JsonNode> februaryPackets = readPackets(archives.archivePaths().get(1));
            assertEquals(1, februaryPackets.size());
            JsonNode februaryJson = februaryPackets.getFirst();
            assertTrue(februaryJson.isArray());
            assertEquals(1, februaryJson.size());
            assertEquals(SQL_STATEMENT_2, februaryJson.get(0).get("sqlQuery").asText());
        }
    }

    @Test
    void writesOnlyMonthsWithLogs() {
        stubColdStorageTask(UPDATED_IN_APRIL);
        DB2AuditloggLineDTO januaryLoggLine = line(JANUARY_TIMESTAMP, SQL_STATEMENT_1);
        DB2AuditloggLineDTO marchLoggLine = line(MARCH_TIMESTAMP, SQL_STATEMENT_2);
        when(proxyService.getDB2AuditloggLinesForDatabaseInDateRange(
                eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(januaryLoggLine, marchLoggLine));

        try (TemporaryLocalArchive archives = service.createLocalArchiveOfColdStorageLogs(auditloggTaskDTO)) {
            assertEquals(List.of(JANUARY_ARCHIVE_PATH, MARCH_ARCHIVE_PATH),
                    archives.archivePaths().stream().map(Path::getFileName).toList());
        }
    }

    @Test
    void returnsNoArchivesWhenThereAreNoLogs() {
        stubColdStorageTask(UPDATED_IN_APRIL);
        when(proxyService.getDB2AuditloggLinesForDatabaseInDateRange(
                eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());

        try (TemporaryLocalArchive archives = service.createLocalArchiveOfColdStorageLogs(auditloggTaskDTO)) {
            assertTrue(archives.archivePaths().isEmpty());
            assertThrows(UnsupportedOperationException.class, () -> archives.archivePaths().add(JANUARY_ARCHIVE_PATH));
        }
    }

    @Test
    void removesOverlappingPaginationRowsAndWritesSeparatePacketsInSameMonthlyArchive() throws IOException {
        stubColdStorageTask(UPDATED_IN_APRIL);
        List<DB2AuditloggLineDTO> firstPage = maxSizePacketOfJanuaryLogs();
        DB2AuditloggLineDTO overlapping = firstPage.getLast();
        DB2AuditloggLineDTO nextJanuaryLoggLine = line(JANUARY_TIMESTAMP.plusDays(1), SQL_STATEMENT_2);
        when(proxyService.getDB2AuditloggLinesForDatabaseInDateRange(
                eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(firstPage, List.of(overlapping, nextJanuaryLoggLine));

        try (TemporaryLocalArchive archives = service.createLocalArchiveOfColdStorageLogs(auditloggTaskDTO)) {
            assertEquals(List.of(JANUARY_ARCHIVE_PATH),
                    archives.archivePaths().stream().map(Path::getFileName).toList());
            List<JsonNode> januaryPackets = readPackets(archives.archivePaths().getFirst());
            assertEquals(2, januaryPackets.size());
            assertEquals(MAX_DB2_PACKET_SIZE, januaryPackets.getFirst().size());
            assertEquals(objectMapper.valueToTree(List.of(nextJanuaryLoggLine)), januaryPackets.getLast());
            verify(proxyService, times(2)).getDB2AuditloggLinesForDatabaseInDateRange(
                    eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class));
        }
    }

    @Test
    void rejectsStalledPaginationAndCleansTemporaryFiles() throws IOException {
        stubColdStorageTask(UPDATED_IN_APRIL);
        DB2AuditloggLineDTO januaryLoggLine = line(PERIOD_START, SQL_STATEMENT_1);
        List<DB2AuditloggLineDTO> fullPage = nCopies(MAX_DB2_PACKET_SIZE, januaryLoggLine);
        when(proxyService.getDB2AuditloggLinesForDatabaseInDateRange(
                eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(fullPage);
        long before = countTemporaryDirectories();

        assertThrows(DB2ColdStorageArchiveService.ColdStoragePaginationException.class,
                () -> service.createLocalArchiveOfColdStorageLogs(auditloggTaskDTO));
        assertEquals(before, countTemporaryDirectories());
    }

    @Test
    void cleansCreatedArchivesWhenProxyFails() throws IOException {
        stubColdStorageTask(UPDATED_IN_APRIL);
        List<DB2AuditloggLineDTO> firstPage = maxSizePacketOfJanuaryLogs();
        RuntimeException proxyFailure = new RuntimeException();
        when(proxyService.getDB2AuditloggLinesForDatabaseInDateRange(
                eq(DB_NAME), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(firstPage)
                .thenThrow(proxyFailure);
        long before = countTemporaryDirectories();

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> service.createLocalArchiveOfColdStorageLogs(auditloggTaskDTO));

        assertEquals("Unable to fetch DB2 audit logs for monthly cold storage", failure.getMessage());
        assertSame(proxyFailure, failure.getCause());
        assertEquals(before, countTemporaryDirectories());
    }

    @Test
    void rejectsNonDb2Tasks() {
        when(auditloggTaskDTO.getUpdated()).thenReturn(UPDATED_IN_APRIL);
        when(auditloggTaskDTO.getTeknologi()).thenReturn(TeknologiEnum.POSTGRESQL);

        assertThrows(InvalidAuditloggTaskStateException.class,
                () -> service.createLocalArchiveOfColdStorageLogs(auditloggTaskDTO));
        verifyNoInteractions(proxyService);
    }

    @Test
    void rejectsTasksNotInColdStorageRequestedState() {
        when(auditloggTaskDTO.getUpdated()).thenReturn(UPDATED_IN_APRIL);
        when(auditloggTaskDTO.getTeknologi()).thenReturn(TeknologiEnum.DB2);
        when(auditloggTaskDTO.getBackfill()).thenReturn(BackfillStatus.BACKFILL_REQUESTED);

        assertThrows(InvalidAuditloggTaskStateException.class,
                () -> service.createLocalArchiveOfColdStorageLogs(auditloggTaskDTO));
        verifyNoInteractions(proxyService);
    }

    private static long countTemporaryDirectories() throws IOException {
        try (var paths = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return paths.filter(path -> path.getFileName().toString()
                    .startsWith(TEMP_DIRECTORY_PREFIX)).count();
        }
    }

    private void stubColdStorageTask(Instant updated) {
        when(auditloggTaskDTO.getUpdated()).thenReturn(updated);
        when(auditloggTaskDTO.getDbname()).thenReturn(DB_NAME);
        when(auditloggTaskDTO.getTeknologi()).thenReturn(TeknologiEnum.DB2);
        when(auditloggTaskDTO.getBackfill()).thenReturn(BackfillStatus.COLD_STORAGE_REQUESTED);
    }

    private static List<DB2AuditloggLineDTO> maxSizePacketOfJanuaryLogs() {
        return IntStream.range(0, MAX_DB2_PACKET_SIZE)
                .mapToObj(index -> line(JANUARY_TIMESTAMP.plusNanos(index), SQL_STATEMENT_1))
                .toList();
    }

    private static DB2AuditloggLineDTO line(LocalDateTime timestamp, String sql) {
        return DB2AuditloggLineDTO.builder()
                .metricsTimestamp(timestamp)
                .databaseName(DB_NAME)
                .tableName("test_table")
                .authId("test_auth_id")
                .sqlQuery(sql)
                .build();
    }

    private List<JsonNode> readPackets(Path archive) throws IOException {
        List<JsonNode> packets = new ArrayList<>();
        try (ZipInputStream zipInput = new ZipInputStream(Files.newInputStream(archive))) {
            while (zipInput.getNextEntry() != null) {
                packets.add(objectMapper.readTree(zipInput.readAllBytes()));
            }
        }
        return packets;
    }
}
