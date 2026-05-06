package no.nav.sikkerhetstjenesten.loggkamel.config;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    public static final String ENTRA_PROXY_BY_NAV_IDENT = "entraProxyByNavIdent";
    public static final String ENTRA_PROXY_BY_T_IDENT = "entraProxyByTIdent";
    public static final String NAIS_GCP_PROJECT_BY_TEAM = "naisGcpProjectByTeam";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(List.of(ENTRA_PROXY_BY_NAV_IDENT, ENTRA_PROXY_BY_T_IDENT, NAIS_GCP_PROJECT_BY_TEAM));
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(15))
                .maximumSize(200));
        return cacheManager;
    }
}

