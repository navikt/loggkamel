package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.util;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZonedDateTime;

@Component
public class GCPTimestampProvider {

    static final Integer MAX_LOG_AGE_IN_DAYS = 29;

    public Instant getTimestampFromLogTime(@Nonnull ZonedDateTime logTime) {

        // If the provided log time is more than 30 days old, GCP will reject the log
        // Instead, provide a new timestamp
        if (ZonedDateTime.now().minusDays(MAX_LOG_AGE_IN_DAYS).isAfter(logTime)) {
            return Instant.now();
        }

        return logTime.toInstant();
    }
}
