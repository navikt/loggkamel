package no.nav.sikkerhetstjenesten.loggkamel.config;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.service.NaisServiceGCP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.web.client.RestClient;

@Configuration
public class NaisClientConfig {

    private static final Logger log = LoggerFactory.getLogger(NaisClientConfig.class);

    @Bean
    @ConditionalOnGCP
    public HttpSyncGraphQlClient naisGraphqlClient() {

        //TODO: remove after troubleshooting
        log.info("Confirming that nais console token is set");
        String naisToken = System.getenv("NAIS_CONSOLE_READ_TOKEN");
        if (naisToken == null || naisToken.isEmpty()) {
            log.error("NAIS_CONSOLE_READ_TOKEN er ikke satt i miljøvariabler. GraphQL klienten vil ikke kunne autentisere mot NAIS Console API.");
        }

        //TODO: get url from properties
        RestClient restClient = RestClient.create("https://console.nav.cloud.nais.io/graphql");
        return HttpSyncGraphQlClient.builder(restClient)
                .headers((headers) -> headers.setBearerAuth(System.getenv("NAIS_CONSOLE_READ_TOKEN")))
                .build();
    }
}
