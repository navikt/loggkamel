package no.nav.sikkerhetstjenesten.loggkamel.client;

public class MockEntraProxyAdapter implements EntraProxyAdapter {
    public static final String MOCK_ANSATT_NAV_TIDENT = "MOCK_NAV_TIDENT";
    public static final String MOCK_ANSATT_VISNING_NAVN = "MOCK_NAVN";
    public static final String MOCK_ANSATT_FORNAVN = "MOCK_FORNAVN";
    public static final String MOCK_ANSATT_ETTERNAVN = "MOCK_ETTERNAVN";
    public static final String MOCK_ANSATT_EPOST = "MOCK_EPOST";
    public static final String MOCK_ENHET_NUMMER = "MOCK_ENHET_NUMMER";
    public static final String MOCK_ENHET_NAVN = "MOCK_ENHET_NAVN";

    @Override
    public EntraProxyAnsatt getAnsattFraNavIdent(String navIdent) {
        return buildMockAnsatt(navIdent);
    }

    @Override
    public EntraProxyAnsatt getAnsattFraTIdent(String tIdent) {
        return buildMockAnsatt(tIdent);
    }

    private EntraProxyAnsatt buildMockAnsatt(String navIdent) {
        return EntraProxyAnsatt.builder()
                .navIdent(navIdent)
                .visningNavn(MOCK_ANSATT_VISNING_NAVN)
                .fornavn(MOCK_ANSATT_FORNAVN)
                .etternavn(MOCK_ANSATT_ETTERNAVN)
                .epost(MOCK_ANSATT_EPOST)
                .enhet(EntraProxyEnhet.builder()
                        .enhetnummer(MOCK_ENHET_NUMMER)
                        .navn(MOCK_ENHET_NAVN)
                        .build())
                .tident(MOCK_ANSATT_NAV_TIDENT)
                .build();
    }

}
