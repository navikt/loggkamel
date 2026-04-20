package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.splitter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessage;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.SharedRouteErrorHandler;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.AuditloggLineMessageHeader.*;
import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer.LogLineMessageProducer.LOG_LINE_MESSAGE_PRODUCER_ROUTE;
import static org.apache.camel.Exchange.FILE_NAME;

@Component
public class LogGroupSplitter extends SharedRouteErrorHandler {

    public static String LOG_GROUP_SPLITTER_ID = "log-group-splitter";
    public static String LOG_GROUP_SPLITTER_ROUTE = "direct:" + LOG_GROUP_SPLITTER_ID;

    @Override
    public void configure() {
        super.errorHandling();

        from(LOG_GROUP_SPLITTER_ROUTE)
            .routeId(LOG_GROUP_SPLITTER_ID)
            .log(LoggingLevel.INFO, "Splitting log file ${header.CamelFileName} into individual messages")
            .split(body().tokenize("^\\<|\n\\<")).streaming()
            .process(exchange -> {
                String originalFileName = exchange.getIn().getHeader(FILE_NAME, String.class);
                String fileExtension = originalFileName.contains(".") ? originalFileName.substring(originalFileName.lastIndexOf('.')) : "";
                String fileBeforeExtension = fileExtension.isEmpty() ? originalFileName : originalFileName.substring(0, originalFileName.lastIndexOf('.'));
                String filenameWithUUID = fileBeforeExtension + "." + UUID.randomUUID() + fileExtension;

                log.info("New filename being assigned: {}", filenameWithUUID);
                exchange.getIn().setHeader(FILE_NAME, filenameWithUUID);
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
                exchange.getMessage().setBody(auditloggLineMessage, AuditloggLineMessage.class);
            })
            .to(LOG_LINE_MESSAGE_PRODUCER_ROUTE);
    }
}
