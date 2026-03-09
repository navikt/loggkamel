package no.nav.sikkerhetstjenesten.loggkamel.client;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EntraProxyEnhet {
    String enhetNummer;
    String navn;
}
