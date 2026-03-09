package no.nav.sikkerhetstjenesten.loggkamel.client;

public class EntraProxyMock implements EntraProxyInterface {
    @Override
    public EntraProxyAnsatt getAnsattMedNavIdent(String bearerToken, String navIdent) {
        //TODO: populate with stock test data
        return EntraProxyAnsatt.builder().build();
    }

    @Override
    public EntraProxyAnsatt getAnsattMedTIdent(String bearerToken, String tIdent) {
        //TODO: populate with stock test data
        return EntraProxyAnsatt.builder().build();
    }
}
