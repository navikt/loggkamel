package no.nav.sikkerhetstjenesten.loggkamel.camel.routes;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import no.nav.sikkerhetstjenesten.loggkamel.service.AuditloggTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer.PostgresLogStreamConsumer.POSTGRES_LOG_CONSUMER_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.config.IdempotentRepositoryConfig.POSTGRES_CONSUMER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostgresLogStreamConsumerRouteTest extends RouteTestSupport {

    private static final String DBNAME = "testdb";
    private static final String STREAM_FILENAME = DBNAME + ".20260115.log";
    private static final String FIRST_PACKET_FILENAME = DBNAME + ".20260115.1.log.packet";

    // The second entry has a continuation line, which belongs to the entry above it
    private static final String STREAM_CONTENT = String.join("\n",
            "<2026-01-15 10:00:00.001 CET:10.0.0.1(5432):v-oidc-z999999-abc@testdb:[1]:LOG:  AUDIT: SESSION,1,1,READ,SELECT,TABLE,public.kunde,select * from kunde,<not logged>",
            "<2026-01-15 10:00:01.002 CET:10.0.0.1(5432):v-oidc-z999999-abc@testdb:[1]:LOG:  AUDIT: SESSION,2,1,WRITE,UPDATE,TABLE,public.kunde,\"update kunde",
            "set navn = 'test'\",<not logged>",
            "<2026-01-15 10:00:02.003 CET:10.0.0.1(5432):v-oidc-z999999-abc@testdb:[1]:LOG:  AUDIT: SESSION,3,1,READ,SELECT,TABLE,public.kunde,select 1,<not logged>"
    ) + "\n";

    @MockitoBean
    AuditloggTaskService auditloggTaskService;

    @BeforeEach
    void startConsumer() throws Exception {
        when(auditloggTaskService.getAuditloggTaskByDbnameAndTeknologi(DBNAME, TeknologiEnum.POSTGRESQL)).thenReturn(activeTask());
        startRoute(POSTGRES_LOG_CONSUMER_ID);
    }

    @Test
    void happyPath_streamIsSplitIntoPacketAndSourceFileIsConsumed() throws Exception {
        dropFileAndAwaitRoute(POSTGRES_LOG_CONSUMER_ID, consumerDir, STREAM_FILENAME, STREAM_CONTENT);

        assertEquals(List.of(FIRST_PACKET_FILENAME), regularFileNames(packetDir));
        JsonNode packet = readJson(packetDir.resolve(FIRST_PACKET_FILENAME));
        assertEquals(3, packet.size());
        assertTrue(packet.get(1).get("body").asText().contains("update kunde\nset navn = 'test'"));
        for (int i = 0; i < packet.size(); i++) {
            assertEquals(i + 1, packet.get(i).get("header").get("placeInPacket").asInt());
            assertEquals("POSTGRESQL", packet.get(i).get("header").get("teknologi").asText());
            assertEquals("sikkerhetstjenesten-dev-f3ab", packet.get(i).get("header").get("teamGcpProjectId").asText());
            assertEquals(DBNAME, packet.get(i).get("header").get("auditloggTaskDTO").get("dbname").asText());
        }
        assertTrue(regularFileNames(consumerDir).isEmpty(), "source file should have been consumed");
        assertTrue(regularFileNames(streamBackoutDir).isEmpty());
        assertEquals(1, claimCount(POSTGRES_CONSUMER));
        verify(auditloggTaskService).registerLogsReceivedForAuditloggTask(DBNAME, TeknologiEnum.POSTGRESQL);
    }

    @Test
    void malformedFile_corruptGzipIsMovedToBackout() throws Exception {
        String corruptFilename = DBNAME + ".20260115.log.gz";
        dropFileAndAwaitRoute(POSTGRES_LOG_CONSUMER_ID, consumerDir, corruptFilename, "this is not gzip data");

        assertEquals(List.of(corruptFilename), regularFileNames(streamBackoutDir));
        assertEquals("this is not gzip data", Files.readString(streamBackoutDir.resolve(corruptFilename)));
        assertTrue(regularFileNames(packetDir).isEmpty());
        verify(auditloggTaskService, never()).registerLogsReceivedForAuditloggTask(anyString(), any());
    }

    @Test
    void gzipCompressedFile_isDecompressedAndProcessed() throws Exception {
        dropFileAndAwaitRoute(POSTGRES_LOG_CONSUMER_ID, consumerDir, STREAM_FILENAME + ".gz", gzip(STREAM_CONTENT));

        assertEquals(List.of(FIRST_PACKET_FILENAME), regularFileNames(packetDir));
        assertEquals(3, readJson(packetDir.resolve(FIRST_PACKET_FILENAME)).size());
        assertTrue(regularFileNames(streamBackoutDir).isEmpty());
    }

    @Test
    void duplicateFilename_isSkippedAndNotReprocessed() throws Exception {
        dropFileAndAwaitRoute(POSTGRES_LOG_CONSUMER_ID, consumerDir, STREAM_FILENAME, STREAM_CONTENT);
        String firstPacketContent = Files.readString(packetDir.resolve(FIRST_PACKET_FILENAME));
        Files.delete(packetDir.resolve(FIRST_PACKET_FILENAME));

        dropFileAndAwaitRoute(POSTGRES_LOG_CONSUMER_ID, consumerDir, STREAM_FILENAME, STREAM_CONTENT);

        assertTrue(regularFileNames(packetDir).isEmpty(), "duplicate must not produce packets again, original was: " + firstPacketContent);
        assertTrue(regularFileNames(streamBackoutDir).isEmpty(), "a duplicate is not an error");
        assertEquals(1, claimCount(POSTGRES_CONSUMER));
        verify(auditloggTaskService, times(1)).registerLogsReceivedForAuditloggTask(DBNAME, TeknologiEnum.POSTGRESQL);
    }

    @Test
    void dependencyException_isRetriedThenMovedToBackout() throws Exception {
        when(auditloggTaskService.getAuditloggTaskByDbnameAndTeknologi(DBNAME, TeknologiEnum.POSTGRESQL))
                .thenThrow(new IllegalStateException("database is down"));

        dropFileAndAwaitRoute(POSTGRES_LOG_CONSUMER_ID, consumerDir, STREAM_FILENAME, STREAM_CONTENT);

        // One initial attempt plus three redeliveries
        verify(auditloggTaskService, times(4)).getAuditloggTaskByDbnameAndTeknologi(DBNAME, TeknologiEnum.POSTGRESQL);
        assertEquals(List.of(STREAM_FILENAME), regularFileNames(streamBackoutDir));
        assertEquals(STREAM_CONTENT, Files.readString(streamBackoutDir.resolve(STREAM_FILENAME)));
        assertTrue(regularFileNames(packetDir).isEmpty());
    }

    private static AuditloggTaskDTO activeTask() {
        return AuditloggTaskDTO.builder()
                .naisteam("testteam")
                .teknologi(TeknologiEnum.POSTGRESQL)
                .dbname(DBNAME)
                .okonomi(false)
                .endringerUtenKrav(false)
                .loggingLeseoperasjoner(true)
                .loggingEndringer(true)
                .fiksa(true)
                .funnetLogger(true)
                .discardLogs(false)
                .backfill(BackfillStatus.BACKFILL_COMPLETED)
                .build();
    }

    private static byte[] gzip(String content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return bytes.toByteArray();
    }
}
