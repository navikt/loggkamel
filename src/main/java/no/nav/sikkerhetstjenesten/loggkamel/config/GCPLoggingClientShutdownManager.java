package no.nav.sikkerhetstjenesten.loggkamel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

/**
 * Ensures that all cached GCP Logging clients are gracefully closed during application shutdown.
 *
 * This handler explicitly closes all Logging instances in the cache before the cache manager is destroyed.
 * This is necessary because:
 * 1. Caffeine's removalListener is not guaranteed to be called during bean destruction
 * 2. Without explicit closure, GCP Logging clients' connection pools may leak
 * 3. Graceful shutdown requires explicit resource cleanup before context closes
 */
public class GCPLoggingClientShutdownManager {

    private static final Logger log = LoggerFactory.getLogger(GCPLoggingClientShutdownManager.class);

    private final CacheManager cacheManager;

    public GCPLoggingClientShutdownManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @EventListener(ContextClosedEvent.class)
    public void onApplicationShutdown() {
        log.info("Application shutdown initiated - closing cached GCP Logging clients");

        Cache gcpLoggingCache = cacheManager.getCache(CacheConfig.GCP_LOGGING_BY_ID);
        if (gcpLoggingCache == null) {
            log.debug("GCP Logging cache not found - no clients to close");
            return;
        }

        try {
            // Clear the cache, which triggers removalListener for all entries
            log.debug("Clearing GCP Logging cache to trigger removalListeners");
            gcpLoggingCache.clear();
            log.info("Successfully closed all cached GCP Logging clients during shutdown");
        } catch (Exception e) {
            log.error("Error during GCP Logging client shutdown - some connections may not be properly closed", e);
        }
    }
}



