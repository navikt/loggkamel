package no.nav.sikkerhetstjenesten.loggkamel.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class EntraProxyAnsatt {
    String navIdent;
    String visningNavn;
    String fornavn;
    String etternavn;
    String ePost;
    EntraProxyEnhet enhet;
    String tIdent;
}
