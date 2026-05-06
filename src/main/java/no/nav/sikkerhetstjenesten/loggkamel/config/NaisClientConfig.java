package no.nav.sikkerhetstjenesten.loggkamel.config;

import no.nav.boot.conditionals.ConditionalOnGCP;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.web.client.RestClient;

@Configuration
public class NaisClientConfig {

    @Bean
    @ConditionalOnGCP
    public HttpSyncGraphQlClient naisGraphqlClient(@Value("${NAIS_CONSOLE_BASE_URL}") String naisConsoleUrl) {
        RestClient restClient = RestClient.create(naisConsoleUrl);
        return HttpSyncGraphQlClient.builder(restClient)
                .headers((headers) -> headers.setBearerAuth(System.getenv("NAIS_CONSOLE_READ_TOKEN")))
                .build();
    }
}
