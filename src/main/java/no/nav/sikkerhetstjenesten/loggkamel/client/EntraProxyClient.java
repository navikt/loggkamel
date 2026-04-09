package no.nav.sikkerhetstjenesten.loggkamel.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface EntraProxyClient {

    @GetExchange("/api/v1/ansatt/{navIdent}")
    EntraProxyAnsatt getAnsattFraNavIdent(@PathVariable("navIdent") String navIdent);

    @GetExchange("/api/v1/ansatt/tident/{tIdent}")
    EntraProxyAnsatt getAnsattFraTIdent(@PathVariable("tIdent") String tIdent);
}
