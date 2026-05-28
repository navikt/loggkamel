package no.nav.sikkerhetstjenesten.loggkamel.client;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.DependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

public class EntraProxyAdapterImpl implements EntraProxyAdapter {

    private final EntraProxyClient client;

    public EntraProxyAdapterImpl(EntraProxyClient client) {
        this.client = client;
    }

    @Cacheable(cacheNames = CacheConfig.ENTRA_PROXY_BY_NAV_IDENT, key = "#navIdent", sync = true)
    public EntraProxyAnsatt getAnsattFraNavIdent(String navIdent) {
        try {
            return client.getAnsattFraNavIdent(navIdent);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }

            throw new DependencyException(e.getMessage(), e);
        }
    }

    @Cacheable(cacheNames = CacheConfig.ENTRA_PROXY_BY_T_IDENT, key = "#tIdent", sync = true)
    public EntraProxyAnsatt getAnsattFraTIdent(String tIdent) {
        try {
            return client.getAnsattFraTIdent(tIdent);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }

            throw new DependencyException(e.getMessage(), e);
        }
    }
}
