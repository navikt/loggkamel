package no.nav.sikkerhetstjenesten.loggkamel.bean;

import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.sikkerhetstjenesten.loggkamel.bean.PgBean.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgBeanTest {

    @Mock
    Exchange exchange;

    @Mock
    Message message;

    @Mock
    EntraProxyAnsatt entraProxyAnsatt;

    @Mock
    EntraProxyService entraProxyService;

    @InjectMocks
    PgBean pgBean;

    @Test
    void extract_invalidLogPattern() {
        String logMessageBody = "blah";

        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(String.class)).thenReturn(logMessageBody);

        assertThrows(RuntimeException.class, () -> pgBean.extract(exchange));
    }

    @Test
    void extract_exceptionFromEntraProxy() {
        String logMessageBody = "<2026-02-10 22:22:17.196 CET:155.55.63.45(36578):v-oidc-A156179-1770758518-xeoEcAD9-axsys-prod-admin@axsys-prod:[2862673]:DBeaver 25.0.4 - Metadata <axsys-prod>> LOG:  AUDIT: SESSION,7,1,READ,SELECT,,,SELECT reltype FROM pg_catalog.pg_class WHERE 1<>1 LIMIT 1,<none>\n";
        String expectedErrorMessage = "Something went wrong, panic!";

        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(String.class)).thenReturn(logMessageBody);

        when(entraProxyService.getAnsattFromNavIdent("A156179")).thenThrow(new RuntimeException(expectedErrorMessage));

        RuntimeException capturedException = assertThrows(RuntimeException.class, () -> pgBean.extract(exchange));
        assertEquals(expectedErrorMessage, capturedException.getMessage());
    }

    @Test
    void extract_happyPath() {
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
        when(entraProxyAnsatt.getEPost()).thenReturn(ePost);

        pgBean.extract(exchange);

        verify(exchange).setVariable(LOG_TIME, logTime);
        verify(exchange).setVariable(NAV_IDENT, navIdent);
        verify(exchange).setVariable(DB_NAME, dbName);
        verify(exchange).setVariable(AUDIT_TYPE, auditType);
        verify(exchange).setVariable(STATEMENT_ID, statementId);
        verify(exchange).setVariable(SUBSTATEMENT_ID, substatementId);
        verify(exchange).setVariable(PG_AUDIT_CLASS, pgAuditClass);
        verify(exchange).setVariable(PG_AUDIT_COMMAND, pgAuditCommand);
        verify(exchange).setVariable(PG_AUDIT_OBJECT_TYPE, pgAuditObjectType);
        verify(exchange).setVariable(PG_AUDIT_OBJECT_NAME, pgAuditObjectName);
        verify(exchange).setVariable(SQL_STATEMENT, sqlStatement);
        verify(exchange).setVariable(SQL_PARAMETER, sqlParameter);
        verify(exchange).setVariable(NAV_EPOST, ePost);
    }

}