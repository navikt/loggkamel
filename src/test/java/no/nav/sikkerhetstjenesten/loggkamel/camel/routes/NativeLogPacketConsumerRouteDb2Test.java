package no.nav.sikkerhetstjenesten.loggkamel.camel.routes;

import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NativeLogPacketConsumerRouteDb2Test extends NativeLogPacketConsumerRouteTestSupport {

    private static final String T_IDENT = "ABC123";

    @Override
    TeknologiEnum teknologi() {
        return TeknologiEnum.DB2;
    }

    @Override
    List<String> logLineBodies() {
        return List.of(
                "{\"metricsTimestamp\":\"2026-01-15T10:00:00\",\"databaseName\":\"TESTDB\",\"tableName\":\"KUNDE\",\"authId\":\"" + T_IDENT + "\",\"sqlQuery\":\"SELECT * FROM KUNDE WHERE ID = 1\"}",
                "{\"metricsTimestamp\":\"2026-01-15T10:00:01\",\"databaseName\":\"TESTDB\",\"tableName\":\"KUNDE\",\"authId\":\"" + T_IDENT + "\",\"sqlQuery\":\"UPDATE KUNDE SET NAVN = 'test' WHERE ID = 1\"}"
        );
    }

    @Override
    void stubEntraProxyFindsAnsatt(String epost) {
        when(entraProxyService.getAnsattFraTIdent(T_IDENT)).thenReturn(ansatt(epost));
    }

    @Override
    void stubEntraProxyFailure() {
        when(entraProxyService.getAnsattFraTIdent(anyString())).thenThrow(new IllegalStateException("entra-proxy is down"));
    }

    @Override
    void verifyEntraProxyLookups(int expectedLookups) {
        verify(entraProxyService, times(expectedLookups)).getAnsattFraTIdent(T_IDENT);
    }

    @Override
    String expectedNavIdent() {
        return null;
    }

    @Override
    String expectedTIdent() {
        return T_IDENT;
    }

    @Override
    List<String> expectedAuditClasses() {
        return List.of("READ", "WRITE");
    }
}
