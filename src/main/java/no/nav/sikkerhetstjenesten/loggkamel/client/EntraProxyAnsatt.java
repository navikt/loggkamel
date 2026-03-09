package no.nav.sikkerhetstjenesten.loggkamel.client;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EntraProxyAnsatt {
    String navIdent;
    String visningNavn;
    String fornavn;
    String etternavn;
    String ePost;
    EntraProxyEnhet enhet;
    String tIdent;
}
