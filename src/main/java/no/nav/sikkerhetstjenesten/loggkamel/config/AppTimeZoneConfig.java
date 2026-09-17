package no.nav.sikkerhetstjenesten.loggkamel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

/**
 * Exposes the application-wide timezone (configured via {@code app.timezone}) as a
 * {@link ZoneId} bean, so date/time calculations in code stay in sync with the
 * {@code zone} used on {@code @Scheduled} cron expressions without repeating the
 * timezone string in multiple places.
 */
@Configuration
public class AppTimeZoneConfig {

    @Bean
    public ZoneId appZoneId(@Value("${app.timezone}") String appTimezone) {
        return ZoneId.of(appTimezone);
    }

}
