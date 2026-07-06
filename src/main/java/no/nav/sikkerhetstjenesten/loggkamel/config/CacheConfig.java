package no.nav.sikkerhetstjenesten.loggkamel.config;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.List;

import com.google.cloud.logging.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    public static final String ENTRA_PROXY_BY_NAV_IDENT = "entraProxyByNavIdent";
    public static final String ENTRA_PROXY_BY_T_IDENT = "entraProxyByTIdent";
    public static final String NAIS_GCP_PROJECT_BY_TEAM = "naisGcpProjectByTeam";
    public static final String GCP_LOGGING_BY_ID = "gcpLoggingByProjectId";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(List.of(
                ENTRA_PROXY_BY_NAV_IDENT,
                ENTRA_PROXY_BY_T_IDENT,
                NAIS_GCP_PROJECT_BY_TEAM,
                GCP_LOGGING_BY_ID));
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(15))
                .maximumSize(200)
                .removalListener((key, value, cause) -> {
                    // Closing GCP Logging instances on eviction from cache
                    if (value instanceof Logging logging) {
                        try {
                            logging.close();
                        } catch (Exception e) {
                            log.warn("Error while closing GCP Logging client for project ID {}: {}",
                                    key, e.getMessage(), e);
                        }
                    }
                })
        );
        return cacheManager;
    }

    @Bean
    public GCPLoggingClientShutdownManager gcpLoggingClientShutdownManager(CacheManager cacheManager) {
        return new GCPLoggingClientShutdownManager(cacheManager);
    }
}

