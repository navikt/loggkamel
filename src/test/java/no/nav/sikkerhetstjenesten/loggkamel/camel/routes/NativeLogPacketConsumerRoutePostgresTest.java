package no.nav.sikkerhetstjenesten.loggkamel.camel.routes;

import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NativeLogPacketConsumerRoutePostgresTest extends NativeLogPacketConsumerRouteTestSupport {

    private static final String NAV_IDENT = "z999999";

    @Override
    TeknologiEnum teknologi() {
        return TeknologiEnum.POSTGRESQL;
    }

    @Override
    List<String> logLineBodies() {
        return List.of(
                "2026-01-15 10:00:00.001 CET:10.0.0.1(5432):v-oidc-z999999-abc@testdb:[1]:LOG:  AUDIT: SESSION,1,1,READ,SELECT,TABLE,public.kunde,select * from kunde,<not logged>",
                "2026-01-15 10:00:01.002 CET:10.0.0.1(5432):v-oidc-z999999-abc@testdb:[1]:LOG:  AUDIT: SESSION,2,1,WRITE,UPDATE,TABLE,public.kunde,\"update kunde set navn = 'test'\",<not logged>"
        );
    }

    @Override
    void stubEntraProxyFindsAnsatt(String epost) {
        when(entraProxyService.getAnsattFraNavIdent(NAV_IDENT)).thenReturn(ansatt(epost));
    }

    @Override
    void stubEntraProxyFailure() {
        when(entraProxyService.getAnsattFraNavIdent(anyString())).thenThrow(new IllegalStateException("entra-proxy is down"));
    }

    @Override
    void verifyEntraProxyLookups(int expectedLookups) {
        verify(entraProxyService, times(expectedLookups)).getAnsattFraNavIdent(NAV_IDENT);
    }

    @Override
    String expectedNavIdent() {
        return NAV_IDENT;
    }

    @Override
    String expectedTIdent() {
        return null;
    }

    @Override
    List<String> expectedAuditClasses() {
        return List.of("READ", "WRITE");
    }
}
