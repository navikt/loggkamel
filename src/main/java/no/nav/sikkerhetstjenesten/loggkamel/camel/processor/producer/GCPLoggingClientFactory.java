package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer;

import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import no.nav.sikkerhetstjenesten.loggkamel.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class GCPLoggingClientFactory {

    @Cacheable(cacheNames = CacheConfig.GCP_LOGGING_BY_ID, key = "#projectId", sync = true)
    public Logging create(String projectId) {
        return LoggingOptions.newBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
    }
}

