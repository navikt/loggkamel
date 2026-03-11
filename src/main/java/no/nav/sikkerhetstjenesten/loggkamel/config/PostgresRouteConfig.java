package no.nav.sikkerhetstjenesten.loggkamel.config;

import no.nav.boot.conditionals.ConditionalOnDev;
import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostgresRouteConfig {

    // Local endpoints
    @Bean(name = "postgresDeadLetterUri")
    @ConditionalOnLocalOrTest
    public String localPostgresDeadLetterUri() {
        return "file:src/main/resources/files/output/dead-letter?fileExist=Append";
    }

    @Bean(name = "postgresInvalidMessagesUri")
    @ConditionalOnLocalOrTest
    public String localPostgresInvalidMessagesUri() {
        return "file:src/main/resources/files/output/invalid-messages?fileExist=Append";
    }

    @Bean(name = "postgresEntranceUri")
    @ConditionalOnLocalOrTest
    public String localPostgresEntranceUri() {
        return "file:src/main/resources/files";
    }

    @Bean(name = "postgresExitUri")
    @ConditionalOnLocalOrTest
    public String localPostgresExitUri() {
        return "file:src/main/resources/files/output/?fileExist=Append";
    }

    // Dev endpoints
    @Bean(name = "postgresDeadLetterUri")
    @ConditionalOnDev
    public String devPostgresDeadLetterUri() {
        return "google-storage://loggkamel-pg-dead-letter-dev";
    }

    @Bean(name = "postgresInvalidMessagesUri")
    @ConditionalOnDev
    public String devPostgresInvalidMessagesUri() {
        return "google-storage://loggkamel-invalid-message-pg-dev";
    }

    @Bean(name = "postgresEntranceUri")
    @ConditionalOnDev
    public String devPostgresEntranceUri() {
        return "google-storage://loggkamel-ingress-pg-dev";
    }

    @Bean(name = "postgresExitUri")
    @ConditionalOnDev
    public String devPostgresExitUri() {
        return "google-storage://loggkamel-output-pg-dev";
    }

    // Prod endpoints


}
