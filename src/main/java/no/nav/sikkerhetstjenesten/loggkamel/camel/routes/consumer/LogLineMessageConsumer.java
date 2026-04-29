package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LoggLineErrorHandler;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import org.apache.camel.LoggingLevel;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.*;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogLineEnricher.LOG_LINE_ENRICHER_ROUTE;
import static org.apache.camel.Exchange.FILE_NAME;

@Component
public class LogLineMessageConsumer extends LoggLineErrorHandler {

    @Autowired
    private Metrics metrics;

    @Autowired
    @Qualifier("logLineMessageIdempotentRepository")
    private JdbcMessageIdRepository idempotentRepository;

    public static String LOG_LINE_MESSAGE_CONSUMER_ID = "log-line-message-consumer";

    @Value("${routing.loggline.bucket}")
    private String consumerUri;

    @Override
    public void configure() {
        super.errorHandling();

        from(consumerUri)
            .routeId(LOG_LINE_MESSAGE_CONSUMER_ID)
            .process(exchange -> {
                // If the file comes from a bucket instead of local storage, still populate the filename
                if (exchange.getIn().getHeader(FILE_NAME, String.class) == null) {
                    exchange.getIn().setHeader(FILE_NAME, exchange.getIn().getHeader("CamelGoogleCloudStorageObjectName", String.class));
                }
            })
            .idempotentConsumer(header(FILE_NAME), idempotentRepository).skipDuplicate(true)
            .log(LoggingLevel.DEBUG, "Received new file from ${header.CamelFileName} with headers ${headers}")
            .log(LoggingLevel.INFO, "Consuming log messages from ${header.CamelFileName}, converting to AuditloggLineMessage")
            .process(exchange -> {
                metrics.intermediateLogConsumed.increment();
            })
            .process(exchange -> {
                AuditloggLineMessage loggLineMessage = objectMapper.readValue(exchange.getMessage().getBody(String.class), AuditloggLineMessage.class);
                exchange.setVariable(TEKNOLOGI, loggLineMessage.getHeader().getTeknologi());
                exchange.setVariable(AUDITLOGG_ARKIV, loggLineMessage.getHeader().getAuditloggArkivResponseDTO());
                exchange.setVariable(TEAM_GCP_PROJECT_ID, loggLineMessage.getHeader().getTeamGcpProjectId());
                exchange.getMessage().setBody(loggLineMessage, AuditloggLineMessage.class);
            })
            .to(LOG_LINE_ENRICHER_ROUTE);
    }
}
