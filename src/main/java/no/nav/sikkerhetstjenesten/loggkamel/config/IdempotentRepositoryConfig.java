package no.nav.sikkerhetstjenesten.loggkamel.config;

import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class IdempotentRepositoryConfig {

    public static final String POSTGRES_CONSUMER = "postgresLogGroupConsumer";
    public static final String LOG_LINE_CONSUMER = "logLineMessageConsumer";

    @Bean
    public JdbcMessageIdRepository postgresLogGroupIdempotentRepository(DataSource dataSource) {
        return new JdbcMessageIdRepository(dataSource, POSTGRES_CONSUMER);
    }

    @Bean
    public JdbcMessageIdRepository logLineMessageIdempotentRepository(DataSource dataSource) {
        return new JdbcMessageIdRepository(dataSource, LOG_LINE_CONSUMER);
    }
}

