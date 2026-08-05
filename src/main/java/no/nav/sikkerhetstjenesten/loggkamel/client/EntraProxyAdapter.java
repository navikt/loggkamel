package no.nav.sikkerhetstjenesten.loggkamel.client;

import no.nav.sikkerhetstjenesten.loggkamel.client.dto.EntraProxyAnsatt;

public interface EntraProxyAdapter {

    EntraProxyAnsatt getAnsattFraNavIdent(String navIdent);

    EntraProxyAnsatt getAnsattFraTIdent(String tIdent);
}
