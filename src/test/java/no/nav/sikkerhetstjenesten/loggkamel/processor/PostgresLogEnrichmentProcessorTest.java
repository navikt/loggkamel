package no.nav.sikkerhetstjenesten.loggkamel.processor;

import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static no.nav.sikkerhetstjenesten.loggkamel.processor.PostgresLogEnrichmentProcessor.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgresLogEnrichmentProcessorTest {

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @Mock
    EntraProxyAnsatt entraProxyAnsatt;

    @Mock
    EntraProxyService entraProxyService;

    @InjectMocks
    PostgresLogEnrichmentProcessor postgresLogEnrichmentProcessor;

    @Test
    void enrich_invalidLogPattern() {
        String logMessageBody = "blah";

        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(String.class)).thenReturn(logMessageBody);

        assertThrows(InvalidIndividualPostgresLog.class, () -> postgresLogEnrichmentProcessor.enrich(exchange));
    }

    @Test
    void enrich_exceptionFromEntraProxy() {
        String logMessageBody = "<2026-02-10 22:22:17.196 CET:155.55.63.45(36578):v-oidc-SAMPLE_NAV_IDENT-1770758518-xeoEcAD9-axsys-prod-admin@axsys-prod:[2862673]:DBeaver 25.0.4 - Metadata <axsys-prod>> LOG:  AUDIT: SESSION,7,1,READ,SELECT,,,SELECT reltype FROM pg_catalog.pg_class WHERE 1<>1 LIMIT 1,<none>\n";
        RuntimeException entraProxyException = new RuntimeException("Something went wrong, panic!");

        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(String.class)).thenReturn(logMessageBody);

        when(entraProxyService.getAnsattFromNavIdent("SAMPLE_NAV_IDENT")).thenThrow(entraProxyException);

        RuntimeException capturedException = assertThrows(RuntimeException.class, () -> postgresLogEnrichmentProcessor.enrich(exchange));
        assertEquals(ENTRA_PROXY_ERROR_MESSAGE, capturedException.getMessage());
        assertEquals(entraProxyException, capturedException.getCause());
    }

    @Test
    void enrich_happyPath() {
        String logTime = "time";
        String navIdent = "navIdent";
        String dbName = "dbName";
        String auditType = "SESSION";
        String statementId = "statementId";
        String substatementId = "substatementId";
        String pgAuditClass = "READ";
        String pgAuditCommand = "SELECT";
        String pgAuditObjectType = "objectType";
        String pgAuditObjectName = "objectName";
        String sqlStatement = "sqlStatement, contains commas, for testing";
        String sqlParameter = "a set of parameters, separated by commas";
        String logMessageBody = String.format("%s(48754):v-oidc-%s-1770722124-C2f1p5OH-axsys-prod-admin@%s:[2704416]:DBeaver 25.0.4 - Metadata <axsys-prod>> LOG:  AUDIT: %s,%s,%s,%s,%s,%s,%s,\"%s\",\"%s\"\n", logTime, navIdent, dbName, auditType, statementId, substatementId, pgAuditClass, pgAuditCommand, pgAuditObjectType, pgAuditObjectName, sqlStatement, sqlParameter);

        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(String.class)).thenReturn(logMessageBody);

        String ePost = "epost";
        when(entraProxyService.getAnsattFromNavIdent(navIdent)).thenReturn(entraProxyAnsatt);
        when(entraProxyAnsatt.getEpost()).thenReturn(ePost);

        postgresLogEnrichmentProcessor.enrich(exchange);

        ArgumentCaptor<Map<String, Object>> logValuesCaptor = ArgumentCaptor.forClass(Map.class);

        verify(exchange).setVariable(eq(LOG_VALUES), logValuesCaptor.capture());

        Map<String, Object> logValues = logValuesCaptor.getValue();
        assertNotNull(logValues);
        assertEquals(logTime, logValues.get(LOG_TIME));
        assertEquals(navIdent, logValues.get(NAV_IDENT));
        assertEquals(dbName, logValues.get(DB_NAME));
        assertEquals(auditType, logValues.get(AUDIT_TYPE));
        assertEquals(statementId, logValues.get(STATEMENT_ID));
        assertEquals(substatementId, logValues.get(SUBSTATEMENT_ID));
        assertEquals(pgAuditClass, logValues.get(PG_AUDIT_CLASS));
        assertEquals(pgAuditCommand, logValues.get(PG_AUDIT_COMMAND));
        assertEquals(pgAuditObjectType, logValues.get(PG_AUDIT_OBJECT_TYPE));
        assertEquals(pgAuditObjectName, logValues.get(PG_AUDIT_OBJECT_NAME));
        assertEquals(sqlStatement, logValues.get(SQL_STATEMENT));
        assertEquals(sqlParameter, logValues.get(SQL_PARAMETER));
        assertEquals(ePost, logValues.get(NAV_EPOST));
    }

}