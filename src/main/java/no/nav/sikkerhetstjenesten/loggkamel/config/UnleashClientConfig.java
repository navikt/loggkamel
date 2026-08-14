package no.nav.sikkerhetstjenesten.loggkamel.config;

import io.getunleash.DefaultUnleash;
import io.getunleash.MoreOperations;
import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import io.getunleash.impactmetrics.MetricsAPI;
import io.getunleash.util.UnleashConfig;
import io.getunleash.variant.Variant;
import lombok.extern.slf4j.Slf4j;
import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.BiPredicate;

@Slf4j
@Configuration
public class UnleashClientConfig {

    @Bean
    @ConditionalOnGCP
    public Unleash unleash() {
        UnleashConfig config = UnleashConfig.builder()
                .appName("loggkamel")
                .unleashAPI(System.getenv("UNLEASH_SERVER_API_URL") + "/api")
                .apiKey(System.getenv("UNLEASH_SERVER_API_TOKEN"))
                .build();

        return new DefaultUnleash(config);
    }

    @Bean
    @ConditionalOnLocalOrTest
    public Unleash unleashMock() {
        return new Unleash() {
            @Override
            public boolean isEnabled(String toggleName, UnleashContext context, BiPredicate<String, UnleashContext> fallbackAction) {
                return true;
            }

            @Override
            public Variant getVariant(String toggleName, UnleashContext context) {
                return null;
            }

            @Override
            public Variant getVariant(String toggleName, UnleashContext context, Variant defaultValue) {
                return null;
            }

            @Override
            public MoreOperations more() {
                return null;
            }

            @Override
            public MetricsAPI getImpactMetrics() {
                return null;
            }
        };
    }
}
