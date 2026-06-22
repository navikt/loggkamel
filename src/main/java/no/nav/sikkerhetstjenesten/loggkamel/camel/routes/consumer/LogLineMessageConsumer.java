package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.consumer.LogLineMessageConsumerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LoggLineErrorHandler;
import org.apache.camel.LoggingLevel;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogLineEnricher.LOG_LINE_ENRICHER_ROUTE;
import static org.apache.camel.Exchange.FILE_NAME;

@Component
public class LogLineMessageConsumer extends LoggLineErrorHandler {

    @Autowired
    @Qualifier("logLineMessageIdempotentRepository")
    private JdbcMessageIdRepository idempotentRepository;

    public static String LOG_LINE_MESSAGE_CONSUMER_ID = "log-line-message-consumer";

    @Value("${routing.loggline.bucket}")
    private String consumerUri;

    @Override
    public void configure() {
        super.errorHandling();

        onException(DuplicateKeyException.class)
                .log("Caught DuplicateKeyException when trying to claim filename: ${headers['CamelFileName']}, dropping message as another instance of loggkamel has successfully claimed it")
                .handled(true);

        from(consumerUri)
            .routeId(LOG_LINE_MESSAGE_CONSUMER_ID)
//            .autoStartup(false)
            .bean(LogLineMessageConsumerProcessor.class, "populateFilenameHeader")
            .log(LoggingLevel.DEBUG, "Received new file from ${header.CamelFileName} with headers ${headers}, file body ${body}")
            .log(LoggingLevel.INFO, "Consuming log messages from ${header.CamelFileName}, converting to AuditloggLineMessage")
            .idempotentConsumer(header(FILE_NAME), idempotentRepository).skipDuplicate(true).removeOnFailure(false)
            .bean(LogLineMessageConsumerProcessor.class, "mapToAuditloggLineMessage")
            .bean(LogLineMessageConsumerProcessor.class, "incrementMetrics")
            .to(LOG_LINE_ENRICHER_ROUTE);
    }
}
