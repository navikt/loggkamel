package no.nav.sikkerhetstjenesten.loggkamel.client;

import lombok.Value;

@Value
public class EntraProxyAnsatt {
    String navIdent;
    String visningNavn;
    String fornavn;
    String etternavn;
    String ePost;
    EntraProxyEnhet enhet;
    String tIdent;
}
