package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.comment.Comment;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidDB2LogPacketException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.AuditloggLineMessageHeader;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto.EnrichedAuditlogg;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.util.DB2AdminUsernameToTIdentConverter;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.DB2AuditloggLineDTO;
import no.nav.sikkerhetstjenesten.loggkamel.client.dto.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    private static final String GENERIC_SQL_STATEMENT = "sql query goes here";

    private static final LocalDateTime LOG_DATE_TIME = LocalDateTime.of(2001, 1, 1, 2, 3, 4);
    private static final String T_IDENT = "tIdent";
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

    @Mock
    DB2AdminUsernameToTIdentConverter db2AdminUsernameToTIdentConverter;

    DB2LogLineEnrichmentProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new DB2LogLineEnrichmentProcessor(metrics, entraProxyService, objectMapper, validator,
                db2AdminUsernameToTIdentConverter);
    }

    private void stubExchange() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(AuditloggLineMessage.class)).thenReturn(auditloggLineMessage);
    }

    @Test
    void enrich_messageBodyMissing() {
        stubExchange();
        when(auditloggLineMessage.getBody()).thenReturn("");

        assertThrows(InvalidDB2LogPacketException.class, () -> processor.enrich(exchange));
    }

    @Test
    void enrich_exceptionMappingLogToDB2DTO() throws JsonProcessingException {
        stubExchange();
        when(auditloggLineMessage.getBody()).thenReturn(DB2_AUDITLOGG_AS_STRING);
        Mockito.doThrow(new JsonProcessingException("blah"){}).when(objectMapper)
                .readValue(DB2_AUDITLOGG_AS_STRING, DB2AuditloggLineDTO.class);

        assertThrows(InvalidDB2LogPacketException.class, () -> processor.enrich(exchange));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "generic sql, WRITE, UNKNOWN",
            "select * from blah where blah, READ, SELECT"
    })
    void enrich_exceptionParsingQueryAsSQLStatement(String sqlStatement, EnrichedAuditlogg.AuditClass auditClass, String pgCommand) throws JsonProcessingException {
        stubExchange();
        when(auditloggLineMessage.getBody()).thenReturn(DB2_AUDITLOGG_AS_STRING);
        when(auditloggLineMessage.getHeader()).thenReturn(auditloggLineMessageHeader);
        when(auditloggLineMessageHeader.getPlaceInPacket()).thenReturn(1);
        Mockito.doReturn(db2AuditloggLineDTO).when(objectMapper).readValue(DB2_AUDITLOGG_AS_STRING, DB2AuditloggLineDTO.class);
        when(db2AuditloggLineDTO.getSqlQuery()).thenReturn(sqlStatement);

        when(db2AuditloggLineDTO.getMetricsTimestamp()).thenReturn(LOG_DATE_TIME);
        when(db2AuditloggLineDTO.getAuthId()).thenReturn(T_IDENT);
        when(db2AuditloggLineDTO.getDatabaseName()).thenReturn(DB_NAME);
        when(db2AdminUsernameToTIdentConverter.replaceWithTIdentIfIsAdminAuthId(T_IDENT)).thenReturn(T_IDENT);
        when(entraProxyService.getAnsattFraTIdent(T_IDENT)).thenReturn(entraProxyAnsatt);
        when(entraProxyAnsatt.getEpost()).thenReturn(EPOST);

        try (MockedStatic<CCJSqlParserUtil> ccjSqlParserUtil = Mockito.mockStatic(CCJSqlParserUtil.class)) {
            ccjSqlParserUtil.when(() -> CCJSqlParserUtil.parse(sqlStatement)).thenThrow(new JSQLParserException("blah"));

            processor.enrich(exchange);

            verify(message).setBody(enrichedAuditloggCaptor.capture());

            EnrichedAuditlogg capturedEnrichedAuditlogg = enrichedAuditloggCaptor.getValue();
            EnrichedAuditlogg expectedEnrichedAuditlogg = buildExpectedEnrichedAuditlogg(sqlStatement, auditClass, pgCommand);

            assertEquals(expectedEnrichedAuditlogg, capturedEnrichedAuditlogg);
        }
    }

    @Test
    void enrich_defaultSQLStatementType() throws JsonProcessingException {
        stubExchange();
        when(auditloggLineMessage.getBody()).thenReturn(DB2_AUDITLOGG_AS_STRING);
        Mockito.doReturn(db2AuditloggLineDTO).when(objectMapper).readValue(DB2_AUDITLOGG_AS_STRING, DB2AuditloggLineDTO.class);
        when(db2AuditloggLineDTO.getSqlQuery()).thenReturn(GENERIC_SQL_STATEMENT);

        when(db2AuditloggLineDTO.getMetricsTimestamp()).thenReturn(LOG_DATE_TIME);
        when(db2AuditloggLineDTO.getAuthId()).thenReturn(T_IDENT);
        when(db2AuditloggLineDTO.getDatabaseName()).thenReturn(DB_NAME);
        when(db2AdminUsernameToTIdentConverter.replaceWithTIdentIfIsAdminAuthId(T_IDENT)).thenReturn(T_IDENT);
        when(entraProxyService.getAnsattFraTIdent(T_IDENT)).thenReturn(entraProxyAnsatt);
        when(entraProxyAnsatt.getEpost()).thenReturn(EPOST);

        try (MockedStatic<CCJSqlParserUtil> ccjSqlParserUtil = Mockito.mockStatic(CCJSqlParserUtil.class)) {
            Comment unexpectedStatementType = Mockito.mock(Comment.class);
            ccjSqlParserUtil.when(() -> CCJSqlParserUtil.parse(GENERIC_SQL_STATEMENT)).thenReturn(unexpectedStatementType);

            processor.enrich(exchange);

            verify(message).setBody(enrichedAuditloggCaptor.capture());

            EnrichedAuditlogg capturedEnrichedAuditlogg = enrichedAuditloggCaptor.getValue();
            EnrichedAuditlogg expectedEnrichedAuditlogg = buildExpectedEnrichedAuditlogg(GENERIC_SQL_STATEMENT, EnrichedAuditlogg.AuditClass.WRITE, "COMMENT");

            assertEquals(expectedEnrichedAuditlogg, capturedEnrichedAuditlogg);
        }
    }

    private EnrichedAuditlogg buildExpectedEnrichedAuditlogg(String sqlStatement, EnrichedAuditlogg.AuditClass auditClass, String pgCommand) {
        return EnrichedAuditlogg.builder()
                .originalMessage(DB2_AUDITLOGG_AS_STRING)
                .sqlStatement(sqlStatement)
                .logTime(LOG_DATE_TIME.atZone(ZoneId.systemDefault()))
                .tIdent(T_IDENT)
                .dbName(DB_NAME)
                .pgAuditClass(auditClass)
                .auditType(EnrichedAuditlogg.AuditType.SESSION)
                .pgCommand(pgCommand)
                .epost(EPOST)
                .build();
    }

}