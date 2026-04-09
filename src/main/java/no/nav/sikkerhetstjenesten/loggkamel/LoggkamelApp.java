package no.nav.sikkerhetstjenesten.loggkamel;

import no.nav.boot.conditionals.Cluster;
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCaching
@EnableOAuth2Client(cacheEnabled = true)
@EnableJwtTokenValidation(ignore = {"org.springdoc", "org.springframework"})
public class LoggkamelApp {
    public static void main(String[] args) {
        var loggKamelApp = new SpringApplication(LoggkamelApp.class);
        loggKamelApp.setAdditionalProfiles(Cluster.profiler());
        loggKamelApp.run(args);
    }
}
