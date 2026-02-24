package no.nav.sikkerhetstjenesten.loggkamel.service;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.sikkerhetstjenesten.loggkamel.config.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.config.EntraProxyInterface;
import org.springframework.stereotype.Service;

@Service
public class EntraProxyService {

    private final EntraProxyInterface entraProxyInterface;
//    private TokenUtil tokenUtil;
    private final MeterRegistry meterRegistry;

    public EntraProxyService(EntraProxyInterface entraProxyInterface,
//                             TokenUtil tokenUtil,
                             MeterRegistry meterRegistry) {
        this.entraProxyInterface = entraProxyInterface;
//        this.tokenUtil = tokenUtil;
        this.meterRegistry = meterRegistry;
    }

    public EntraProxyAnsatt getAnsattFromNavIdent(String navIdent) {
        // TODO: fetch token from token util
        String token = "blah";

        // TODO: elaborate with request time recording in meter registry, error handling
        return entraProxyInterface.getAnsattMedNavIdent(token, navIdent);
    }
}
