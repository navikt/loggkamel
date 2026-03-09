package no.nav.sikkerhetstjenesten.loggkamel.config;

import no.nav.security.token.support.client.spring.oauth2.ClientConfigurationPropertiesMatcher;
import no.nav.security.token.support.client.spring.oauth2.OAuth2ClientRequestInterceptor;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AuthConfig {
    @Bean
    public RestClientCustomizer customizer(OAuth2ClientRequestInterceptor reqInterceptor) {
        return restClientBuilder -> restClientBuilder.requestInterceptor(reqInterceptor);
    }

    //TODO: figure out under what circumstances we'd want to configure this ourselves, how to avoid bean declaration conflict
//    @Bean
//    public OAuth2ClientRequestInterceptor localOAuth2ClientRequestInterceptor(ClientConfigurationProperties properties, OAuth2AccessTokenService service, ClientConfigurationPropertiesMatcher matcher) {
//        return new OAuth2ClientRequestInterceptor(properties, service, matcher);
//    }

    @Bean
    public ClientConfigurationPropertiesMatcher clientConfigurationPropertiesMatcher() {
        return new ClientConfigurationPropertiesMatcher() {};
    }
}
