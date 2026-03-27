package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyClient;
import org.springframework.stereotype.Service;

@Service
public class EntraProxyService {

    private final EntraProxyClient client;

    public EntraProxyService(EntraProxyClient client) {
        this.client = client;
    }

    public EntraProxyAnsatt getAnsattFromNavIdent(String navIdent) {
        // TODO: elaborate with request time recording in meter registry, error handling
        return client.getAnsattMedNavIdent(navIdent);
    }
}
