package no.nav.sikkerhetstjenesten.loggkamel.config;

import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class IdempotentRepositoryConfig {

    @Bean
    public JdbcMessageIdRepository postgresLogGroupIdempotentRepository(DataSource dataSource) {
        return new JdbcMessageIdRepository(dataSource, "postgresLogGroupConsumer");
    }

    @Bean
    public JdbcMessageIdRepository logLineMessageIdempotentRepository(DataSource dataSource) {
        return new JdbcMessageIdRepository(dataSource, "logLineMessageConsumer");
    }
}

