package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.EntraProxyDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.PostgresLogLineEnrichmentProcessor.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgresLogLineEnrichmentProcessorTest {

    private static final String logTime = "time";
    private static final String navIdent = "navIdent";
    private static final String dbName = "dbName";
    private static final String auditType = "SESSION";
    private static final String statementId = "statementId";
    private static final String substatementId = "substatementId";
    private static final String pgAuditClass = "READ";
    private static final String pgAuditCommand = "SELECT";
    private static final String pgAuditObjectType = "objectType";
    private static final String pgAuditObjectName = "objectName";
    private static final String sqlStatement = "sqlStatement, contains commas, for testing";
    private static final String sqlParameter = "a set of parameters, separated by commas";

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @Mock
    LogRoutingAttributes logRoutingAttributes;

    @Mock
    EntraProxyAnsatt entraProxyAnsatt;

    @Mock
    EntraProxyService entraProxyService;

    @Mock
    LogRoutingAttributesEnricher logRoutingAttributesEnricher;

    @InjectMocks
    PostgresLogLineEnrichmentProcessor postgresLogLineEnrichmentProcessor;

    @Test
    void enrich_invalidLogPattern() {
        String logMessageBody = "blah";

        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(String.class)).thenReturn(logMessageBody);

        assertThrows(InvalidPostgresLogLineException.class, () -> postgresLogLineEnrichmentProcessor.enrich(exchange));
    }

    @Test
    void enrich_exceptionFromEntraProxy() {
        String logMessageBody = "<2026-02-10 22:22:17.196 CET:155.55.63.45(36578):v-oidc-SAMPLE_NAV_IDENT-1770758518-xeoEcAD9-axsys-prod-admin@axsys-prod:[2862673]:DBeaver 25.0.4 - Metadata <axsys-prod>> LOG:  AUDIT: SESSION,7,1,READ,SELECT,,,SELECT reltype FROM pg_catalog.pg_class WHERE 1<>1 LIMIT 1,<none>\n";
        RuntimeException entraProxyException = new RuntimeException("Something went wrong, panic!");

        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(String.class)).thenReturn(logMessageBody);

        when(entraProxyService.getAnsattFraNavIdent("SAMPLE_NAV_IDENT")).thenThrow(entraProxyException);

        EntraProxyDependencyException capturedException = assertThrows(EntraProxyDependencyException.class, () -> postgresLogLineEnrichmentProcessor.enrich(exchange));
        assertEquals(ENTRA_PROXY_ERROR_MESSAGE, capturedException.getMessage());
        assertEquals(entraProxyException, capturedException.getCause());
    }

    @Test
    void enrich_noEmployeeInfo() {
        String logMessageBody = "<2026-02-10 22:22:17.196 CET:155.55.63.45(36578):v-oidc-SAMPLE_NAV_IDENT-1770758518-xeoEcAD9-axsys-prod-admin@axsys-prod:[2862673]:DBeaver 25.0.4 - Metadata <axsys-prod>> LOG:  AUDIT: SESSION,7,1,READ,SELECT,,,SELECT reltype FROM pg_catalog.pg_class WHERE 1<>1 LIMIT 1,<none>\n";
        RuntimeException entraProxyException = new RuntimeException("Something went wrong, panic!");

        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(String.class)).thenReturn(logMessageBody);

        when(entraProxyService.getAnsattFraNavIdent("SAMPLE_NAV_IDENT")).thenReturn(null);

        InvalidPostgresLogLineException capturedException = assertThrows(InvalidPostgresLogLineException.class, () -> postgresLogLineEnrichmentProcessor.enrich(exchange));
    }

    @Test
    void enrich_happyPath() {
        String logMessageBody = String.format("%s(48754):v-oidc-%s-1770722124-C2f1p5OH-axsys-prod-admin@%s:[2704416]:DBeaver 25.0.4 - Metadata <axsys-prod>> LOG:  AUDIT: %s,%s,%s,%s,%s,%s,%s,\"%s\",\"%s\"\n", logTime, navIdent, dbName, auditType, statementId, substatementId, pgAuditClass, pgAuditCommand, pgAuditObjectType, pgAuditObjectName, sqlStatement, sqlParameter);

        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(String.class)).thenReturn(logMessageBody);

        String ePost = "epost";
        when(entraProxyService.getAnsattFraNavIdent(navIdent)).thenReturn(entraProxyAnsatt);
        when(entraProxyAnsatt.getEpost()).thenReturn(ePost);

        when(logRoutingAttributesEnricher.constructRoutingAttributesFromAuditClass(pgAuditClass)).thenReturn(logRoutingAttributes);

        postgresLogLineEnrichmentProcessor.enrich(exchange);

        ArgumentCaptor<PostgresEnrichmentAttributes> logEnrichmentCaptor = ArgumentCaptor.forClass(PostgresEnrichmentAttributes.class);
        verify(exchange).setVariable(eq(LOG_ENRICHMENT), logEnrichmentCaptor.capture());
        verify(exchange).setProperty(eq(LogRoutingAttributes.LOG_ROUTING_ATTRIBUTES), eq(logRoutingAttributes));

        PostgresEnrichmentAttributes capturedLogEnrichment = logEnrichmentCaptor.getValue();
        assertEquals(expectedLogEnrichment(), capturedLogEnrichment);

    }

    private PostgresEnrichmentAttributes expectedLogEnrichment() {
        return PostgresEnrichmentAttributes.builder()
                .logTime(logTime)
                .navIdent(navIdent)
                .dbName(dbName)
                .auditType(auditType)
                .statementId(statementId)
                .substatementId(substatementId)
                .pgAuditClass(pgAuditClass)
                .pgCommand(pgAuditCommand)
                .pgObjectType(pgAuditObjectType)
                .pgObjectName(pgAuditObjectName)
                .sqlStatement(sqlStatement)
                .sqlParameters(sqlParameter)
                .epost(entraProxyAnsatt.getEpost())
                .build();
    }

}