package no.nav.sikkerhetstjenesten.loggkamel.config;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnLocalOrTest
@EnableMockOAuth2Server
public class LocalSecurityConfiguration {
}
