package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LoggLineErrorHandler;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import org.apache.camel.LoggingLevel;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.*;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogLineEnricher.LOG_LINE_ENRICHER_ROUTE;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

@Component
public class LogLineMessageConsumer extends LoggLineErrorHandler {

    @Autowired
    private Metrics metrics;

    @Autowired
    @Qualifier("logLineMessageIdempotentRepository")
    private JdbcMessageIdRepository idempotentRepository;

    public static String LOG_LINE_MESSAGE_CONSUMER_ID = "log-line-message-consumer";

    @Value("${routing.loggline.queue.read}")
    private String consumerUri;

    @Override
    public void configure() {
        super.errorHandling();

        from(consumerUri)
            .routeId(LOG_LINE_MESSAGE_CONSUMER_ID)
//            .autoStartup(false)
            .process(exchange -> {
                // If the file comes from a bucket instead of local storage, still populate the filename
                if (exchange.getIn().getHeader(FILE_NAME, String.class) == null) {
                    String[] filenameSplitByDirectories = exchange.getIn().getHeader(OBJECT_NAME, String.class).split("/");
                    String filenameWithoutDirectories = filenameSplitByDirectories[filenameSplitByDirectories.length - 1];
                    exchange.getIn().setHeader(FILE_NAME, filenameWithoutDirectories);
                }
            })
            .idempotentConsumer(header(FILE_NAME), idempotentRepository).skipDuplicate(true).removeOnFailure(false)
            .log(LoggingLevel.DEBUG, "Received new file from ${header.CamelFileName} with headers ${headers}")
            .log(LoggingLevel.INFO, "Consuming log messages from ${header.CamelFileName}, converting to AuditloggLineMessage")
            .process(exchange -> {
                AuditloggLineMessage loggLineMessage = objectMapper.readValue(exchange.getMessage().getBody(String.class), AuditloggLineMessage.class);
                exchange.setVariable(TEKNOLOGI, loggLineMessage.getHeader().getTeknologi());
                exchange.setVariable(AUDITLOGG_ARKIV, loggLineMessage.getHeader().getAuditloggArkivResponseDTO());
                exchange.setVariable(TEAM_GCP_PROJECT_ID, loggLineMessage.getHeader().getTeamGcpProjectId());
                exchange.getMessage().setBody(loggLineMessage, AuditloggLineMessage.class);
            })
            .process(exchange -> {
                metrics.incrementHappyPath(Metrics.Multiplicity.single, exchange.getVariable(TEKNOLOGI, String.class), Metrics.Action.consumed);
                String dbName = exchange.getVariable(AUDITLOGG_ARKIV, AuditloggArkivResponseDTO.class).getDbname();
                metrics.incrementDatabaseSpecificAction(dbName,  exchange.getVariable(TEKNOLOGI, String.class), Metrics.Action.consumed);
            })
            .to(LOG_LINE_ENRICHER_ROUTE);
    }
}
