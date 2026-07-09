package no.nav.sikkerhetstjenesten.loggkamel.config;

import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class IdempotentRepositoryConfig {

    public static final String POSTGRES_CONSUMER = "postgresLogStreamConsumer";
    public static final String LOG_PACKET_CONSUMER = "standardizedLogPacketConsumer";

    @Bean
    public JdbcMessageIdRepository postgresLogStreamIdempotentRepository(DataSource dataSource) {
        return new JdbcMessageIdRepository(dataSource, POSTGRES_CONSUMER);
    }

    @Bean
    public JdbcMessageIdRepository logPacketIdempotentRepository(DataSource dataSource) {
        return new JdbcMessageIdRepository(dataSource, LOG_PACKET_CONSUMER);
    }
}

