package no.nav.sikkerhetstjenesten.loggkamel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.web.client.RestClient;

@Configuration
public class NaisClientConfig {

    @Bean
    public HttpSyncGraphQlClient naisGraphqlClient() {
        RestClient restClient = RestClient.create("https://console.nav.cloud.nais.io/graphql");
        return HttpSyncGraphQlClient.builder(restClient)
                //TODO: get token from nais secrets
                .headers((headers) -> headers.setBearerAuth("PLACEHOLDER_FOR_NAIS_TOKEN"))
                .build();
    }
}
