package no.nav.sikkerhetstjenesten.loggkamel.camel.routes;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.BackfillStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer.NativeLogPacketConsumer.NATIVE_LOG_PACKET_CONSUMER_ID;
import static no.nav.sikkerhetstjenesten.loggkamel.config.IdempotentRepositoryConfig.LOG_PACKET_CONSUMER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

/** Scenarios shared by every log packet technology. Subclasses supply the technology-specific packet contents. */
abstract class NativeLogPacketConsumerRouteTestSupport extends RouteTestSupport {

    static final String PACKET_FILENAME = "testdb.20260115.1.log.packet";
    static final String EPOST = "test.bruker@example.com";

    abstract TeknologiEnum teknologi();

    /** The raw body of each log line in the packet, as the stream splitter would have produced it. */
    abstract List<String> logLineBodies();

    abstract void stubEntraProxyFindsAnsatt(String epost);

    abstract void stubEntraProxyFailure();

    abstract void verifyEntraProxyLookups(int expectedLookups);

    abstract String expectedNavIdent();

    abstract String expectedTIdent();

    abstract List<String> expectedAuditClasses();

    @BeforeEach
    void startConsumer() throws Exception {
        startRoute(NATIVE_LOG_PACKET_CONSUMER_ID);
    }

    @Test
    void happyPath_everyLineIsEnrichedAndWrittenAsItsOwnFile() throws Exception {
        stubEntraProxyFindsAnsatt(EPOST);

        dropFileAndAwaitRoute(NATIVE_LOG_PACKET_CONSUMER_ID, packetDir, PACKET_FILENAME, validPacket());

        assertEquals(outputFilenames(logLineBodies().size()), regularFileNames(outputDir));
        for (int i = 0; i < logLineBodies().size(); i++) {
            JsonNode line = readJson(outputDir.resolve(outputFilenames(logLineBodies().size()).get(i)));
            assertEquals(expectedNavIdent(), line.get("navIdent").asText(null));
            assertEquals(expectedTIdent(), line.get("tIdent").asText(null));
            assertEquals(EPOST, line.get("epost").asText());
            assertEquals("testdb", line.get("dbName").asText().toLowerCase());
            assertEquals(expectedAuditClasses().get(i), line.get("pgAuditClass").asText());
            assertEquals("dbAuditEntry", line.get("requestType").asText());
        }
        assertTrue(regularFileNames(packetDir).isEmpty(), "source packet should have been consumed");
        assertTrue(regularFileNames(packetBackoutDir).isEmpty());
        assertEquals(1, claimCount(LOG_PACKET_CONSUMER));
    }

    @Test
    void malformedFile_isMovedToBackoutWithoutProducingLines() throws Exception {
        String malformed = "this is not a json list of log lines";

        dropFileAndAwaitRoute(NATIVE_LOG_PACKET_CONSUMER_ID, packetDir, PACKET_FILENAME, malformed);

        assertEquals(List.of(PACKET_FILENAME), regularFileNames(packetBackoutDir));
        assertEquals(malformed, Files.readString(packetBackoutDir.resolve(PACKET_FILENAME)));
        assertTrue(regularFileNames(outputDir).isEmpty());
        verifyNoInteractions(entraProxyService);
    }

    @Test
    void duplicateFilename_isSkippedAndNotReprocessed() throws Exception {
        stubEntraProxyFindsAnsatt(EPOST);
        dropFileAndAwaitRoute(NATIVE_LOG_PACKET_CONSUMER_ID, packetDir, PACKET_FILENAME, validPacket());
        for (String producedFile : regularFileNames(outputDir)) {
            Files.delete(outputDir.resolve(producedFile));
        }

        dropFileAndAwaitRoute(NATIVE_LOG_PACKET_CONSUMER_ID, packetDir, PACKET_FILENAME, validPacket());

        assertTrue(regularFileNames(outputDir).isEmpty(), "duplicate must not produce lines again");
        assertTrue(regularFileNames(packetBackoutDir).isEmpty(), "a duplicate is not an error");
        assertTrue(regularFileNames(packetDir).isEmpty());
        assertEquals(1, claimCount(LOG_PACKET_CONSUMER));
        verifyEntraProxyLookups(logLineBodies().size());
    }

    @Test
    void dependencyException_isRetriedThenMovedToBackout() throws Exception {
        stubEntraProxyFailure();

        dropFileAndAwaitRoute(NATIVE_LOG_PACKET_CONSUMER_ID, packetDir, PACKET_FILENAME, validPacket());

        assertEquals(List.of(PACKET_FILENAME), regularFileNames(packetBackoutDir));
        assertEquals(validPacket(), Files.readString(packetBackoutDir.resolve(PACKET_FILENAME)));
        assertTrue(regularFileNames(outputDir).isEmpty());
        // One initial attempt plus three redeliveries
        verifyEntraProxyLookups(4);
    }

    static EntraProxyAnsatt ansatt(String epost) {
        return EntraProxyAnsatt.builder().epost(epost).build();
    }

    private List<String> outputFilenames(int lineCount) {
        List<String> filenames = new ArrayList<>();
        for (int place = 1; place <= lineCount; place++) {
            filenames.add(PACKET_FILENAME.replace(".packet", "." + place + ".packet"));
        }
        return filenames;
    }

    private String validPacket() throws IOException {
        List<AuditloggLineMessage> lines = new ArrayList<>();
        for (int i = 0; i < logLineBodies().size(); i++) {
            lines.add(AuditloggLineMessage.builder()
                    .body(logLineBodies().get(i))
                    .header(AuditloggLineMessageHeader.builder()
                            .teknologi(teknologi())
                            .teamGcpProjectId("sikkerhetstjenesten-dev-f3ab")
                            .auditloggTaskDTO(activeTask())
                            .placeInPacket(i + 1)
                            .build())
                    .build());
        }
        return objectMapper.writeValueAsString(lines);
    }

    private AuditloggTaskDTO activeTask() {
        return AuditloggTaskDTO.builder()
                .naisteam("testteam")
                .teknologi(teknologi())
                .dbname("testdb")
                .okonomi(false)
                .arkivforskrift(false)
                .endringerUtenKrav(false)
                .loggingLeseoperasjoner(true)
                .loggingEndringer(true)
                .fiksa(true)
                .funnetLogger(true)
                .discardLogs(false)
                .backfill(BackfillStatus.BACKFILL_COMPLETED)
                .build();
    }
}
