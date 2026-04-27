package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LoggGroupErrorHandler;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.*;
import static no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics.INTERMEDIATE_LOG_LINE_ACTION;

@Component
public class LogLineMessageProducer extends LoggGroupErrorHandler {

    @Value("${routing.loggline.bucket}")
    String logLineMessageBucketUri;

    public static String LOG_LINE_MESSAGE_PRODUCER = "loggline-producer";
    public static String LOG_LINE_MESSAGE_PRODUCER_ROUTE = "direct:" + LOG_LINE_MESSAGE_PRODUCER;

    @Override
    public void configure() {
        super.errorHandling();

        //TODO: update filename to make clear that you are producing a log line message, not the original file
        from(LOG_LINE_MESSAGE_PRODUCER_ROUTE)
                .routeId(LOG_LINE_MESSAGE_PRODUCER)
                .log("Producing loggline message ${header.CamelFileName} to log line endpoint")
                .process(exchange -> {
                    INTERMEDIATE_LOG_LINE_ACTION.labelValues("produced").inc();
                })
                .process(exchange -> {
                    AuditloggLineMessage auditloggLineMessage = AuditloggLineMessage.builder()
                            .body(exchange.getMessage().getBody(String.class))
                            .header(AuditloggLineMessageHeader.builder()
                                    .teknologi(exchange.getVariable(TEKNOLOGI, TeknologiEnum.class))
                                    .teamGcpProjectId(exchange.getVariable(TEAM_GCP_PROJECT_ID, String.class))
                                    .auditloggArkivResponseDTO(exchange.getVariable(AUDITLOGG_ARKIV, AuditloggArkivResponseDTO.class))
                                    .build())
                            .build();
                    exchange.getMessage().setBody(objectMapper.writeValueAsString(auditloggLineMessage));
                })
                .toD(logLineMessageBucketUri);
    }
}
