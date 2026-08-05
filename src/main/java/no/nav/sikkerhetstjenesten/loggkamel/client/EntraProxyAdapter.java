package no.nav.sikkerhetstjenesten.loggkamel.client;

public interface EntraProxyAdapter {

    EntraProxyAnsatt getAnsattFraNavIdent(String navIdent);

    EntraProxyAnsatt getAnsattFraTIdent(String tIdent);
}
