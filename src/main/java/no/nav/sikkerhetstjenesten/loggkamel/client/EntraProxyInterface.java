package no.nav.sikkerhetstjenesten.loggkamel.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface EntraProxyInterface {

    // TODO: update compilation to preserve path variable names, such that you don't have to explicitly declare it here
    @GetExchange("/api/v1/ansatt/{navIdent}")
    EntraProxyAnsatt getAnsattMedNavIdent(@PathVariable("navIdent") String navIdent);

    @GetExchange("/api/v1/ansatt/tident/{tIdent}")
    EntraProxyAnsatt getAnsattMedTIdent(@PathVariable String tIdent);
}
