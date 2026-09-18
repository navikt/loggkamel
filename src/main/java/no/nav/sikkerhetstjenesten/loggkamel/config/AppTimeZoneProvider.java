package no.nav.sikkerhetstjenesten.loggkamel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class AppTimeZoneProvider {

    @Bean
    public ZoneId appZoneId(@Value("${app.timezone}") String appTimezone) {
        return ZoneId.of(appTimezone);
    }

}
