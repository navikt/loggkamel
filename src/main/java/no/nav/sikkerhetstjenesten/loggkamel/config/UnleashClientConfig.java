package no.nav.sikkerhetstjenesten.loggkamel.config;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.util.UnleashConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class UnleashClientConfig {

    @Bean
    public Unleash unleash() {
        UnleashConfig config = UnleashConfig.builder()
                .appName("loggkamel")
                .unleashAPI(System.getenv("UNLEASH_SERVER_API_URL") + "/api")
                .apiKey(System.getenv("UNLEASH_SERVER_API_TOKEN"))
                .build();

        return new DefaultUnleash(config);
    }
}
