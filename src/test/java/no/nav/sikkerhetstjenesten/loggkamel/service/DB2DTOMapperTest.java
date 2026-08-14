package no.nav.sikkerhetstjenesten.loggkamel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DB2DTOMapperTest {

    private static final String LOGGLINE_1_AS_STRING = "I am the first log line";
    private static final String LOGGLINE_2_AS_STRING = "I am the second log line";

    private static final String DB_NAME = "db name";
    private static final String GCP_ID = "gcp id";

    @Mock
    DB2AuditloggLineDTO db2AuditloggLineDTO1;

    @Mock
    DB2AuditloggLineDTO db2AuditloggLineDTO2;

    List<DB2AuditloggLineDTO> db2AuditloggLineDTOs;

    @Mock
    AuditloggTaskDTO  auditloggTaskDTO;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    private DB2DTOMapper db2DTOMapper;

    @BeforeEach
    void setUp() {
        db2AuditloggLineDTOs = List.of(db2AuditloggLineDTO1, db2AuditloggLineDTO2);
    }

    @Test
    void convertDB2DTOsToAuditloggLineMessages_exceptionOnConvertingToJson() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(db2AuditloggLineDTO1)).thenThrow(new JsonProcessingException("oh noooo"){});
        when(auditloggTaskDTO.getDbname()).thenReturn(DB_NAME);

        assertThrows(InvalidLogLineException.class, () -> db2DTOMapper.convertDB2DTOsToAuditloggLineMessages(db2AuditloggLineDTOs, auditloggTaskDTO, GCP_ID));
    }

    @Test
    void convertDB2DTOsToAuditloggLineMessages_happyPath() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(db2AuditloggLineDTO1)).thenReturn(LOGGLINE_1_AS_STRING);
        when(objectMapper.writeValueAsString(db2AuditloggLineDTO2)).thenReturn(LOGGLINE_2_AS_STRING);

        List<AuditloggLineMessage> convertedLogglines = db2DTOMapper.convertDB2DTOsToAuditloggLineMessages(db2AuditloggLineDTOs, auditloggTaskDTO, GCP_ID);

        assertEquals(2,  convertedLogglines.size());
        assertEquals(LOGGLINE_1_AS_STRING, convertedLogglines.get(0).getBody());
        assertEquals(LOGGLINE_2_AS_STRING, convertedLogglines.get(1).getBody());

        AuditloggLineMessageHeader header =  convertedLogglines.get(0).getHeader();
        assertEquals(GCP_ID, header.getTeamGcpProjectId());
        assertEquals(auditloggTaskDTO,  header.getAuditloggTaskDTO());
        assertEquals(TeknologiEnum.DB2, header.getTeknologi());
    }

}