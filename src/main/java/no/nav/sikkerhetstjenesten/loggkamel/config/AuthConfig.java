package no.nav.sikkerhetstjenesten.loggkamel.config;

import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;
import no.nav.security.token.support.client.spring.oauth2.ClientConfigurationPropertiesMatcher;
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client;
import no.nav.security.token.support.client.spring.oauth2.OAuth2ClientRequestInterceptor;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableOAuth2Client(cacheEnabled = true)
//@EnableJwtTokenValidation
@Configuration
class AuthConfig {
    @Bean
    public RestClientCustomizer customizer(OAuth2ClientRequestInterceptor reqInterceptor) {
        return restClientBuilder -> restClientBuilder.requestInterceptor(reqInterceptor);
    }

    @Bean
    public OAuth2ClientRequestInterceptor localOAuth2ClientRequestInterceptor(ClientConfigurationProperties properties, OAuth2AccessTokenService service, ClientConfigurationPropertiesMatcher matcher) {
        return new OAuth2ClientRequestInterceptor(properties, service, matcher);
    }

    @Bean
    public ClientConfigurationPropertiesMatcher clientConfigurationPropertiesMatcher() {
        return new ClientConfigurationPropertiesMatcher() {};
    }
}
