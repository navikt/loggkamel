package no.nav.sikkerhetstjenesten.loggkamel.config;

import no.nav.security.token.support.client.spring.oauth2.OAuth2ClientRequestInterceptor;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonRestClientConfig {

    @Bean
    public RestClientCustomizer restClientCustomizer(OAuth2ClientRequestInterceptor interceptor) {
        return restClientBuilder -> restClientBuilder.requestInterceptor(interceptor);
    }
}
