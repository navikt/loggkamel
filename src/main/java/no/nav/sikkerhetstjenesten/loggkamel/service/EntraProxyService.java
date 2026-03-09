package no.nav.sikkerhetstjenesten.loggkamel.service;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyInterface;
import org.springframework.stereotype.Service;

@Service
public class EntraProxyService {

    private final EntraProxyInterface entraProxyInterface;
    private final MeterRegistry meterRegistry;

    public EntraProxyService(EntraProxyInterface entraProxyInterface,
                             MeterRegistry meterRegistry) {
        this.entraProxyInterface = entraProxyInterface;
        this.meterRegistry = meterRegistry;
    }

    public EntraProxyAnsatt getAnsattFromNavIdent(String navIdent) {
        // TODO: elaborate with request time recording in meter registry, error handling
        return entraProxyInterface.getAnsattMedNavIdent(navIdent);
    }
}
