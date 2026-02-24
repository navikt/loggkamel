package no.nav.sikkerhetstjenesten.loggkamel.bean;

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

    @InjectMocks
    PgBean pgBean;

    @Test
    void extract_patternNotFound() {
        String logMessageBody = "blah";

        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(String.class)).thenReturn(logMessageBody);

        assertThrows(RuntimeException.class, () -> pgBean.extract(exchange));
    }

    @Test
    void extract_patternFound() {
        String logTime = "time";
        String navIdent = "navIdent";
        String dbName = "dbName";
        String logType = "READ";
        String sqlCommand = "sqlCommand";
        String sqlParameters = "<none>";
        String logMessageBody = String.format("%s(48754):v-oidc-%s-1770722124-C2f1p5OH-axsys-prod-admin@%s:[2704416]:DBeaver 25.0.4 - Metadata <axsys-prod>> LOG:  AUDIT: SESSION,1,1,%s,SELECT,,,\"%s\",%s\n", logTime, navIdent, dbName, logType, sqlCommand, sqlParameters);

        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(String.class)).thenReturn(logMessageBody);

        pgBean.extract(exchange);

        verify(exchange).setVariable(LOG_TIME, logTime);
        verify(exchange).setVariable(NAV_IDENT, navIdent);
        verify(exchange).setVariable(DB_NAME, dbName);
        verify(exchange).setVariable(LOG_TYPE, logType);
        verify(exchange).setVariable(SQL_COMMAND, sqlCommand);
        verify(exchange).setVariable(SQL_PARAMS, sqlParameters);
    }

}