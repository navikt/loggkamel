package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import jakarta.validation.Validator;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.EntraProxyDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.PostgresLogLineEnrichmentProcessor.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgresLogLineEnrichmentProcessorTest {

    private static final String LOG_TIME = "2026-04-09 16:51:06.264 CEST";
    private static final ZonedDateTime LOG_TIME_AS_ZONEDDATETIME = ZonedDateTime.parse(LOG_TIME, PostgresLogLineEnrichmentProcessor.DATE_TIME_FORMATTER);
    private static final String NAV_IDENT = "navIdent";
    private static final String DB_NAME = "dbName";
    private static final EnrichedAuditlogg.AuditType AUDIT_TYPE = EnrichedAuditlogg.AuditType.SESSION;
    private static final String STATEMENT_ID = "statementId";
    private static final String SUBSTATEMENT_ID = "substatementId";
    private static final EnrichedAuditlogg.AuditClass PG_AUDIT_CLASS = EnrichedAuditlogg.AuditClass.READ;
    private static final String PG_AUDIT_COMMAND = "SELECT";
    private static final String PG_AUDIT_OBJECT_TYPE = "objectType";
    private static final String PG_AUDIT_OBJECT_NAME = "objectName";
    private static final String SQL_STATEMENT = "sqlStatement, contains commas, for testing";
    private static final String SQL_PARAMETER = "a set of parameters, separated by commas";
    private static final String ANSATT_EPOST = "ansattEpost";

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @Mock
    AuditloggLineMessage auditloggLineMessage;

    @Mock
    LogLineOperationTypes logLineOperationTypes;

    @Mock
    EntraProxyAnsatt entraProxyAnsatt;

    @Mock
    EntraProxyService entraProxyService;

    @Mock
    LogLineOperationsEnricher logLineOperationsEnricher;

    @Mock
    Metrics metrics;

    @Mock
    Validator validator;

    @InjectMocks
    PostgresLogLineEnrichmentProcessor postgresLogLineEnrichmentProcessor;

    @BeforeEach
    void setup() {
        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(AuditloggLineMessage.class)).thenReturn(auditloggLineMessage);
    }

    @Test
    void enrich_invalidLogPattern() {
        String logMessageBody = "blah";

        when(auditloggLineMessage.getBody()).thenReturn(logMessageBody);

        assertThrows(InvalidPostgresLogLineException.class, () -> postgresLogLineEnrichmentProcessor.enrich(exchange));
    }

    @Test
    void enrich_exceptionFromEntraProxy() {
        String logMessageBody = "2026-02-10 22:22:17.196 CET:155.55.63.45(36578):v-oidc-SAMPLENAVIDENT-1770758518-xeoEcAD9-axsys-prod-admin@axsys-prod:[2862673]:DBeaver 25.0.4 - Metadata <axsys-prod>> LOG:  AUDIT: SESSION,7,1,READ,SELECT,,,SELECT reltype FROM pg_catalog.pg_class WHERE 1<>1 LIMIT 1,<none>\n";
        RuntimeException entraProxyException = new RuntimeException("Something went wrong, panic!");

        when(auditloggLineMessage.getBody()).thenReturn(logMessageBody);

        when(entraProxyService.getAnsattFraNavIdent("SAMPLENAVIDENT")).thenThrow(entraProxyException);

        EntraProxyDependencyException capturedException = assertThrows(EntraProxyDependencyException.class, () -> postgresLogLineEnrichmentProcessor.enrich(exchange));
        assertEquals(ENTRA_PROXY_ERROR_MESSAGE, capturedException.getMessage());
        assertEquals(entraProxyException, capturedException.getCause());
    }

    @Test
    void enrich_noEmployeeInfo() {
        String logMessageBody = "2026-02-10 22:22:17.196 CET:155.55.63.45(36578):v-oidc-SAMPLENAVIDENT-1770758518-xeoEcAD9-axsys-prod-admin@axsys-prod:[2862673]:DBeaver 25.0.4 - Metadata <axsys-prod>> LOG:  AUDIT: SESSION,7,1,READ,SELECT,,,SELECT reltype FROM pg_catalog.pg_class WHERE 1<>1 LIMIT 1,<none>\n";

        when(auditloggLineMessage.getBody()).thenReturn(logMessageBody);

        when(entraProxyService.getAnsattFraNavIdent("SAMPLENAVIDENT")).thenReturn(null);

        when(logLineOperationsEnricher.constructOperationTypesFromAuditClass(PG_AUDIT_CLASS)).thenReturn(logLineOperationTypes);

        postgresLogLineEnrichmentProcessor.enrich(exchange);

        ArgumentCaptor<EnrichedAuditlogg> logEnrichmentCaptor = ArgumentCaptor.forClass(EnrichedAuditlogg.class);
        verify(message).setBody(logEnrichmentCaptor.capture());

        EnrichedAuditlogg capturedLogEnrichment = logEnrichmentCaptor.getValue();
        assertNull(capturedLogEnrichment.getEpost());
    }

    @ParameterizedTest
    @MethodSource("provideLogLinesWithValidFormats")
    void enrich_happyPath(String logMessageBody) {
        when(auditloggLineMessage.getBody()).thenReturn(logMessageBody);

        when(entraProxyService.getAnsattFraNavIdent(NAV_IDENT)).thenReturn(entraProxyAnsatt);
        when(entraProxyAnsatt.getEpost()).thenReturn(ANSATT_EPOST);

        when(logLineOperationsEnricher.constructOperationTypesFromAuditClass(PG_AUDIT_CLASS)).thenReturn(logLineOperationTypes);

        postgresLogLineEnrichmentProcessor.enrich(exchange);

        ArgumentCaptor<EnrichedAuditlogg> logEnrichmentCaptor = ArgumentCaptor.forClass(EnrichedAuditlogg.class);
        verify(message).setBody(logEnrichmentCaptor.capture());
        verify(exchange).setVariable(eq(LogLineOperationTypes.LOG_LINE_OPERATION_TYPES), eq(logLineOperationTypes));

        EnrichedAuditlogg capturedLogEnrichment = logEnrichmentCaptor.getValue();
        assertEquals(expectedLogEnrichment(logMessageBody), capturedLogEnrichment);
    }

    private static Stream<String> provideLogLinesWithValidFormats() {
        return Stream.of(
                // v-oidc- formatted string contains navIdent
                String.format("%s:127.0.0.1(48754):v-oidc-%s-1770722124-C2f1p5OH-axsys-prod-admin@%s:[2704416]:DBeaver 25.0.4 - Metadata <axsys-prod>> LOG:  AUDIT: %s,%s,%s,%s,%s,%s,%s,\"%s\",\"%s\"\n", LOG_TIME, NAV_IDENT, DB_NAME, AUDIT_TYPE.name(), STATEMENT_ID, SUBSTATEMENT_ID, PG_AUDIT_CLASS.name(), PG_AUDIT_COMMAND, PG_AUDIT_OBJECT_TYPE, PG_AUDIT_OBJECT_NAME, SQL_STATEMENT, SQL_PARAMETER),
                // Ident field with unknown formatting, we use the whole thing as a navIdent
                String.format("%s:127.0.0.1(48754):%s@%s:[2704416]:DBeaver 25.0.4 - Metadata <axsys-prod>> LOG:  AUDIT: %s,%s,%s,%s,%s,%s,%s,\"%s\",\"%s\"\n", LOG_TIME, NAV_IDENT, DB_NAME, AUDIT_TYPE.name(), STATEMENT_ID, SUBSTATEMENT_ID, PG_AUDIT_CLASS.name(), PG_AUDIT_COMMAND, PG_AUDIT_OBJECT_TYPE, PG_AUDIT_OBJECT_NAME, SQL_STATEMENT, SQL_PARAMETER),
                // Instead of giving an IP address and process number, the running location is given as [local]
                String.format("%s:[local]:v-oidc-%s-1770722124-C2f1p5OH-axsys-prod-admin@%s:[2704416]:DBeaver 25.0.4 - Metadata <axsys-prod>> LOG:  AUDIT: %s,%s,%s,%s,%s,%s,%s,\"%s\",\"%s\"\n", LOG_TIME, NAV_IDENT, DB_NAME, AUDIT_TYPE.name(), STATEMENT_ID, SUBSTATEMENT_ID, PG_AUDIT_CLASS.name(), PG_AUDIT_COMMAND, PG_AUDIT_OBJECT_TYPE, PG_AUDIT_OBJECT_NAME, SQL_STATEMENT, SQL_PARAMETER)
        );
    }

    private EnrichedAuditlogg expectedLogEnrichment(String logMessageBody) {
        return EnrichedAuditlogg.builder()
                .originalMessage(logMessageBody)
                .logTime(LOG_TIME_AS_ZONEDDATETIME)
                .navIdent(NAV_IDENT)
                .dbName(DB_NAME)
                .auditType(AUDIT_TYPE)
                .statementId(STATEMENT_ID)
                .substatementId(SUBSTATEMENT_ID)
                .pgAuditClass(PG_AUDIT_CLASS)
                .pgCommand(PG_AUDIT_COMMAND)
                .pgObjectType(PG_AUDIT_OBJECT_TYPE)
                .pgObjectName(PG_AUDIT_OBJECT_NAME)
                .sqlStatement(SQL_STATEMENT)
                .sqlParameters(SQL_PARAMETER)
                .epost(ANSATT_EPOST)
                .build();
    }

}