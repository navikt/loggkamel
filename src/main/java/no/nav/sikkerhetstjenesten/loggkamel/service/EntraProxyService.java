package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyClient;
import no.nav.sikkerhetstjenesten.loggkamel.config.EntraProxyCacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
public class EntraProxyService {

    private final EntraProxyClient client;

    public EntraProxyService(EntraProxyClient client) {
        this.client = client;
    }

    @Cacheable(cacheNames = EntraProxyCacheConfig.ENTRA_PROXY_BY_NAV_IDENT, key = "#navIdent", sync = true)
    public EntraProxyAnsatt getAnsattFraNavIdent(String navIdent) {
        try {
            return client.getAnsattFraNavIdent(navIdent);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }

            throw e;
        }
    }

    @Cacheable(cacheNames = EntraProxyCacheConfig.ENTRA_PROXY_BY_T_IDENT, key = "#tIdent", sync = true)
    public EntraProxyAnsatt getAnsattFraTIdent(String tIdent) {
        try {
            return client.getAnsattFraTIdent(tIdent);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }

            throw e;
        }
    }
}
