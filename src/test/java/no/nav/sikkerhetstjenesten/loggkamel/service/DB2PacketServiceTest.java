package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.packet.PacketPersistenceService;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import no.nav.sikkerhetstjenesten.loggkamel.service.naisservice.NaisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import static no.nav.sikkerhetstjenesten.loggkamel.service.LoggkamelProxyService.MAX_DB2_PACKET_SIZE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DB2PacketServiceTest {

    private static final String GCP_ID = "gcpId";
    private static final String DB_NAME = "dbName";
    private static final String NAISTEAM = "naisteam";

    @Mock
    DB2AuditloggLineDTO db2AuditloggLineDTO1;

    @Mock
    DB2AuditloggLineDTO db2AuditloggLineDTO2;

    @Mock
    DB2AuditloggLineDTO db2AuditloggLineDTO3;

    @Mock
    DB2AuditloggLineDTO db2AuditloggLineDTO4;

    @Mock
    AuditloggTaskDTO  auditloggTaskDTO;

    @Mock
    AuditloggLineMessage auditloggLineMessage;

    @Mock
    PacketPersistenceService packetPersistenceService;

    @Mock
    LoggkamelProxyService loggkamelProxyService;

    @Mock
    NaisService naisService;

    @Mock
    DB2DTOMapper db2DTOMapper;

    @InjectMocks
    DB2PacketService db2PacketService;

    @Test
    void persistPacketWithGivenDate_convertsAndPersistsAGivenListOfDTOsWithCorrectFilename() {
        List<DB2AuditloggLineDTO> db2AuditloggLines = List.of(db2AuditloggLineDTO1);
        when(db2DTOMapper.convertDB2DTOsToAuditloggLineMessages(db2AuditloggLines, auditloggTaskDTO, GCP_ID)).thenReturn(List.of(auditloggLineMessage));
        when(auditloggTaskDTO.getDbname()).thenReturn(DB_NAME);

        LocalDate givenDate = LocalDate.of(2025, Month.APRIL, 3);
        String givenDateAsString = "20250403";
        db2PacketService.persistPacketWithGivenDate(db2AuditloggLines, givenDate, auditloggTaskDTO, GCP_ID);

        ArgumentCaptor<String> filenameCaptor =  ArgumentCaptor.forClass(String.class);
        verify(packetPersistenceService).saveAuditloggLineMessagesWithFilename(filenameCaptor.capture(), eq(List.of(auditloggLineMessage)));
        String capturedFilename = filenameCaptor.getValue();
        String[] capturedFilenameSplit = capturedFilename.split("\\.");

        assertEquals(DB_NAME, capturedFilenameSplit[0]);
        assertEquals(givenDateAsString, capturedFilenameSplit[1]);
    }

    @Test
    void persistAuditloggLinesAsPacketsSeparatedByDate_groupsDTOsByDate() {
        when(auditloggTaskDTO.getNaisteam()).thenReturn(NAISTEAM);
        when(naisService.getCurrentEnvGCPIDForTeam(NAISTEAM)).thenReturn(GCP_ID);

        List<DB2AuditloggLineDTO> auditloggLineDTOs = List.of(db2AuditloggLineDTO1,  db2AuditloggLineDTO2, db2AuditloggLineDTO3);

        LocalDateTime dateTime1 = LocalDateTime.of(2025, Month.APRIL, 3, 1, 2, 3);
        LocalDateTime dateTime2 = LocalDateTime.of(2025, Month.APRIL, 4, 1, 2, 3);

        when(db2AuditloggLineDTO1.getMetricsTimestamp()).thenReturn(dateTime1);

        when(db2AuditloggLineDTO2.getMetricsTimestamp()).thenReturn(dateTime2);
        when(db2AuditloggLineDTO3.getMetricsTimestamp()).thenReturn(dateTime2);

        db2PacketService.persistAuditloggLinesAsPacketsSeparatedByDate(auditloggLineDTOs, auditloggTaskDTO);

        verify(db2DTOMapper).convertDB2DTOsToAuditloggLineMessages(List.of(db2AuditloggLineDTO1),  auditloggTaskDTO, GCP_ID);
        verify(db2DTOMapper).convertDB2DTOsToAuditloggLineMessages(List.of(db2AuditloggLineDTO2, db2AuditloggLineDTO3),   auditloggTaskDTO, GCP_ID);
    }

    @Test
    void fetchLogsWithinDateRangeAndPersistAsPackets_filtersOutDuplicateLogsBetweenPackets() {
        //Testing case where there are two packets consisting with timestamps aaaaaabbb, bbbb'b'b'b'b'ccc
        // Log lines with timestamp b that were already in the first packet, should be filtered out before the second packet is persisted

        LocalDateTime dateTime1 = LocalDateTime.of(2025, Month.APRIL, 3, 1, 2, 3);
        LocalDateTime dateTime2 = LocalDateTime.of(2025, Month.APRIL, 4, 1, 2, 3);
        LocalDateTime dateTime3 = LocalDateTime.of(2025, Month.APRIL, 5, 1, 2, 3);

        LocalDateTime date1AtStartOfDay = dateTime1.toLocalDate().atStartOfDay();
        LocalDateTime date3AtEndOfDay = dateTime3.toLocalDate().atTime(LocalTime.MAX);

        when(auditloggTaskDTO.getDbname()).thenReturn(DB_NAME);
        when(auditloggTaskDTO.getNaisteam()).thenReturn(NAISTEAM);
        when(naisService.getCurrentEnvGCPIDForTeam(NAISTEAM)).thenReturn(GCP_ID);

        List<DB2AuditloggLineDTO> packet1 = buildPacketWithFiveTrailingSharedTimestamps(dateTime1, dateTime2);
        when(loggkamelProxyService.getDB2AuditloggLinesForDatabaseInDateRange(DB_NAME, date1AtStartOfDay, date3AtEndOfDay)).thenReturn(packet1);
        List<DB2AuditloggLineDTO> packet2 = buildPacketWithTenLeadingSharedTimestampsFiveOfWhichHaveBeenUsed(dateTime2, dateTime3);
        when(loggkamelProxyService.getDB2AuditloggLinesForDatabaseInDateRange(DB_NAME, dateTime2, date3AtEndOfDay)).thenReturn(packet2);

        db2PacketService.fetchLogsWithinDateRangeAndPersistAsPackets(auditloggTaskDTO, dateTime1.toLocalDate(), dateTime3.toLocalDate());

        List<DB2AuditloggLineDTO> dtosWithFirstTimestamp = new ArrayList<>();
        for (int i = 0; i < MAX_DB2_PACKET_SIZE - 5; i++) {
            dtosWithFirstTimestamp.add(db2AuditloggLineDTO1);
        }

        List<DB2AuditloggLineDTO> dtosWithSecondTimestampInFirstPacket = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            dtosWithSecondTimestampInFirstPacket.add(db2AuditloggLineDTO2);
        }

        List<DB2AuditloggLineDTO> dtosWithSecondTimestampWithoutRepetitions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            dtosWithSecondTimestampWithoutRepetitions.add(db2AuditloggLineDTO3);
        }

        List<DB2AuditloggLineDTO> dtosWithThirdTimestamp = new ArrayList<>();
        for (int i = 0; i < MAX_DB2_PACKET_SIZE - 10 - 1; i++) {
            dtosWithThirdTimestamp.add(db2AuditloggLineDTO4);
        }

        verify(db2DTOMapper).convertDB2DTOsToAuditloggLineMessages(dtosWithFirstTimestamp, auditloggTaskDTO, GCP_ID);
        verify(db2DTOMapper).convertDB2DTOsToAuditloggLineMessages(dtosWithSecondTimestampInFirstPacket, auditloggTaskDTO, GCP_ID);
        verify(db2DTOMapper).convertDB2DTOsToAuditloggLineMessages(dtosWithSecondTimestampWithoutRepetitions, auditloggTaskDTO, GCP_ID);
        verify(db2DTOMapper).convertDB2DTOsToAuditloggLineMessages(dtosWithThirdTimestamp, auditloggTaskDTO, GCP_ID);
        verifyNoMoreInteractions(db2DTOMapper);
    }

    private List<DB2AuditloggLineDTO> buildPacketWithFiveTrailingSharedTimestamps(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        when(db2AuditloggLineDTO1.getMetricsTimestamp()).thenReturn(dateTime1);
        when(db2AuditloggLineDTO2.getMetricsTimestamp()).thenReturn(dateTime2);

        List<DB2AuditloggLineDTO> packet1 = new ArrayList<>();

        for (int i = 0; i < MAX_DB2_PACKET_SIZE - 5; i++) {
            packet1.add(db2AuditloggLineDTO1);
        }

        for (int i = 0; i < 5; i++) {
            packet1.add(db2AuditloggLineDTO2);
        }

        return packet1;
    }

    private List<DB2AuditloggLineDTO> buildPacketWithTenLeadingSharedTimestampsFiveOfWhichHaveBeenUsed(LocalDateTime dateTime2,  LocalDateTime dateTime3) {
        when(db2AuditloggLineDTO3.getMetricsTimestamp()).thenReturn(dateTime2);
        when(db2AuditloggLineDTO4.getMetricsTimestamp()).thenReturn(dateTime3);

        List<DB2AuditloggLineDTO> packet2 = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            packet2.add(db2AuditloggLineDTO2);
            packet2.add(db2AuditloggLineDTO3);
        }

        //this packet is almost but not quite at the packet size limit
        for (int i = 0; i < MAX_DB2_PACKET_SIZE - 10 - 1; i++) {
            packet2.add(db2AuditloggLineDTO4);
        }

        return packet2;
    }

}