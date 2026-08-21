package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.comment.Comment;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidDB2LogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.EnrichedAuditlogg;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DB2LogLineEnrichmentProcessorTest {

    private static final String DB2_AUDITLOGG_AS_STRING = "structured json representing a db2 auditlogg object";
    private static final String SQL_QUERY = "sql query goes here";

    private static final LocalDateTime LOG_DATE_TIME = LocalDateTime.of(2001, 1, 1, 2, 3, 4);
    private static final String NAV_IDENT = "navIdent";
    private static final String DB_NAME = "dbName";
    private static final String EPOST = "epost";

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @Mock
    AuditloggLineMessage auditloggLineMessage;

    @Mock
    AuditloggLineMessageHeader auditloggLineMessageHeader;

    @Mock
    DB2AuditloggLineDTO db2AuditloggLineDTO;

    @Mock
    EntraProxyAnsatt entraProxyAnsatt;

    @Captor
    ArgumentCaptor<EnrichedAuditlogg> enrichedAuditloggCaptor;

    @Mock
    Metrics metrics;

    @Mock
    EntraProxyService entraProxyService;

    @Mock
    Validator validator;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    DB2LogLineEnrichmentProcessor processor;

    @BeforeEach
    void setUp() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(AuditloggLineMessage.class)).thenReturn(auditloggLineMessage);
    }

    @Test
    void enrich_messageBodyMissing() {
        when(auditloggLineMessage.getBody()).thenReturn("");

        assertThrows(InvalidDB2LogLineException.class, () -> processor.enrich(exchange));
    }

    @Test
    void enrich_exceptionMappingLogToDB2DTO() throws JsonProcessingException {
        when(auditloggLineMessage.getBody()).thenReturn(DB2_AUDITLOGG_AS_STRING);
        when(objectMapper.readValue(DB2_AUDITLOGG_AS_STRING, DB2AuditloggLineDTO.class)).thenThrow(new JsonProcessingException("blah"){});

        assertThrows(InvalidDB2LogLineException.class, () -> processor.enrich(exchange));
    }

    @Test
    void enrich_exceptionParsingQueryAsSQLStatement() throws JsonProcessingException {
        when(auditloggLineMessage.getBody()).thenReturn(DB2_AUDITLOGG_AS_STRING);
        when(auditloggLineMessage.getHeader()).thenReturn(auditloggLineMessageHeader);
        when(auditloggLineMessageHeader.getPlaceInPacket()).thenReturn(1);
        when(objectMapper.readValue(DB2_AUDITLOGG_AS_STRING, DB2AuditloggLineDTO.class)).thenReturn(db2AuditloggLineDTO);
        when(db2AuditloggLineDTO.getSqlQuery()).thenReturn(SQL_QUERY);

        when(db2AuditloggLineDTO.getMetricsTimestamp()).thenReturn(LOG_DATE_TIME);
        when(db2AuditloggLineDTO.getAuthId()).thenReturn(NAV_IDENT);
        when(db2AuditloggLineDTO.getDatabaseName()).thenReturn(DB_NAME);
        when(entraProxyService.getAnsattFraNavIdent(NAV_IDENT)).thenReturn(entraProxyAnsatt);
        when(entraProxyAnsatt.getEpost()).thenReturn(EPOST);

        try (MockedStatic<CCJSqlParserUtil> ccjSqlParserUtil = Mockito.mockStatic(CCJSqlParserUtil.class)) {
            ccjSqlParserUtil.when(() -> CCJSqlParserUtil.parse(SQL_QUERY)).thenThrow(new JSQLParserException("blah"));

            processor.enrich(exchange);

            verify(message).setBody(enrichedAuditloggCaptor.capture());

            EnrichedAuditlogg capturedEnrichedAuditlogg = enrichedAuditloggCaptor.getValue();
            EnrichedAuditlogg expectedEnrichedAuditlogg = buildExpectedEnrichedAuditlogg(EnrichedAuditlogg.AuditClass.WRITE, "UNKNOWN");

            assertEquals(expectedEnrichedAuditlogg, capturedEnrichedAuditlogg);
        }
    }

    @Test
    void enrich_defaultSQLStatementType() throws JsonProcessingException {
        when(auditloggLineMessage.getBody()).thenReturn(DB2_AUDITLOGG_AS_STRING);
        when(objectMapper.readValue(DB2_AUDITLOGG_AS_STRING, DB2AuditloggLineDTO.class)).thenReturn(db2AuditloggLineDTO);
        when(db2AuditloggLineDTO.getSqlQuery()).thenReturn(SQL_QUERY);

        when(db2AuditloggLineDTO.getMetricsTimestamp()).thenReturn(LOG_DATE_TIME);
        when(db2AuditloggLineDTO.getAuthId()).thenReturn(NAV_IDENT);
        when(db2AuditloggLineDTO.getDatabaseName()).thenReturn(DB_NAME);
        when(entraProxyService.getAnsattFraNavIdent(NAV_IDENT)).thenReturn(entraProxyAnsatt);
        when(entraProxyAnsatt.getEpost()).thenReturn(EPOST);

        try (MockedStatic<CCJSqlParserUtil> ccjSqlParserUtil = Mockito.mockStatic(CCJSqlParserUtil.class)) {
            Comment unexpectedStatementType = Mockito.mock(Comment.class);
            ccjSqlParserUtil.when(() -> CCJSqlParserUtil.parse(SQL_QUERY)).thenReturn(unexpectedStatementType);

            processor.enrich(exchange);

            verify(message).setBody(enrichedAuditloggCaptor.capture());

            EnrichedAuditlogg capturedEnrichedAuditlogg = enrichedAuditloggCaptor.getValue();
            EnrichedAuditlogg expectedEnrichedAuditlogg = buildExpectedEnrichedAuditlogg(EnrichedAuditlogg.AuditClass.MISC, "COMMENT");

            assertEquals(expectedEnrichedAuditlogg, capturedEnrichedAuditlogg);
        }
    }

    private EnrichedAuditlogg buildExpectedEnrichedAuditlogg(EnrichedAuditlogg.AuditClass auditClass, String pgCommand) {
        return EnrichedAuditlogg.builder()
                .originalMessage(DB2_AUDITLOGG_AS_STRING)
                .sqlStatement(SQL_QUERY)
                .logTime(LOG_DATE_TIME.atZone(ZoneId.systemDefault()))
                .navIdent(NAV_IDENT)
                .dbName(DB_NAME)
                .pgAuditClass(auditClass)
                .auditType(EnrichedAuditlogg.AuditType.SESSION)
                .pgCommand(pgCommand)
                .epost(EPOST)
                .build();
    }

}