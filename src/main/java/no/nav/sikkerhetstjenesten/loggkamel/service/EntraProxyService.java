package no.nav.sikkerhetstjenesten.loggkamel.service;

import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAdapter;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class EntraProxyService {

    private final EntraProxyAdapter entraProxyAdapter;

    public EntraProxyService(EntraProxyAdapter entraProxyAdapter) {
        this.entraProxyAdapter = entraProxyAdapter;
    }

    @Cacheable(cacheNames = CacheConfig.ENTRA_PROXY_BY_NAV_IDENT, key = "#navIdent", sync = true)
    public EntraProxyAnsatt getAnsattFraNavIdent(String navIdent) {
        return entraProxyAdapter.getAnsattFraNavIdent(navIdent);
    }

    @Cacheable(cacheNames = CacheConfig.ENTRA_PROXY_BY_T_IDENT, key = "#tIdent", sync = true)
    public EntraProxyAnsatt getAnsattFraTIdent(String tIdent) {
        return entraProxyAdapter.getAnsattFraTIdent(tIdent);
    }
}
