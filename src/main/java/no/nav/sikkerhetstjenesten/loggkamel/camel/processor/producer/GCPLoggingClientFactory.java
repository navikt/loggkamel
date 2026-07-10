package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer;

import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import org.springframework.stereotype.Service;

@Service
public class GCPLoggingClientFactory {

    public Logging create(String projectId) {
        return LoggingOptions.newBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
    }
}

