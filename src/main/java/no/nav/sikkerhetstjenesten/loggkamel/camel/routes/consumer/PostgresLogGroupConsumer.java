package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.PostgresLogGroupConsumerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LoggGroupErrorHandler;
import org.apache.camel.LoggingLevel;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogGroupEnricher.LOG_GROUP_ENRICHER_ROUTE;
import static org.apache.camel.Exchange.FILE_NAME;

@Component
public class PostgresLogGroupConsumer extends LoggGroupErrorHandler {

    public static String POSTGRES_LOG_CONSUMER_ID = "postgres-log-consumer";

    @Value("${routing.postgres.consumer}")
    private String consumerUri;

    @Autowired
    @Qualifier("postgresLogGroupIdempotentRepository")
    private JdbcMessageIdRepository idempotentRepository;

    @Override
    public void configure() {
        super.errorHandling();

        onException(DuplicateKeyException.class)
                .log("Caught DuplicateKeyException when trying to claim filename: ${headers['CamelFileName']}, dropping message as another instance of loggkamel has successfully claimed it")
                .handled(true);

        from(consumerUri)
            .routeId(POSTGRES_LOG_CONSUMER_ID)
            .autoStartup(false)
            .bean(PostgresLogGroupConsumerProcessor.class, "initializeConsumerState")
            .log(LoggingLevel.INFO, "Consuming postgres log messages as filename: ${header.CamelFileName}")
            .log(LoggingLevel.DEBUG, "Received new file from ${header.CamelFileName} with headers ${headers}, file body ${body}")
            .idempotentConsumer(header(FILE_NAME), idempotentRepository).skipDuplicate(true).removeOnFailure(false) //Prevent multiple instances of loggkamel from processing the same file
            .bean(PostgresLogGroupConsumerProcessor.class, "incrementMetrics")
            .bean(PostgresLogGroupConsumerProcessor.class, "decompressIfGzip")
            .to(LOG_GROUP_ENRICHER_ROUTE);
    }
}
