package no.nav.sikkerhetstjenesten.loggkamel;

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
@EnableMockOAuth2Server
public class LocalSecurityConfiguration {
}
