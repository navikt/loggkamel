package no.nav.sikkerhetstjenesten.loggkamel.config;

import no.nav.boot.conditionals.ConditionalOnGCP;
import no.nav.sikkerhetstjenesten.loggkamel.auth.NaisConsoleAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.web.client.RestClient;

@Configuration
public class NaisClientConfig {

    @Bean
    @ConditionalOnGCP
    public HttpSyncGraphQlClient naisGraphqlClient(@Value("${NAIS_CONSOLE_BASE_URL}") String naisConsoleUrl,
                                                   NaisConsoleAuthInterceptor naisConsoleAuthInterceptor) {
        RestClient restClient = RestClient.builder()
                .baseUrl(naisConsoleUrl)
                .requestInterceptors(interceptors -> interceptors.add(naisConsoleAuthInterceptor))
                .build();

        return HttpSyncGraphQlClient.builder(restClient)
                .build();
    }
}
