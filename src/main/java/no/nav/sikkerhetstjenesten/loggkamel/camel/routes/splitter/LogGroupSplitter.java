package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.splitter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LoggGroupErrorHandler;
import org.apache.camel.LoggingLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer.LogLineMessageProducer.LOG_LINE_MESSAGE_PRODUCER_ROUTE;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.component.google.storage.GoogleCloudStorageConstants.OBJECT_NAME;

@Component
public class LogGroupSplitter extends LoggGroupErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(LogGroupSplitter.class);

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
                exchange.getIn().setHeader(OBJECT_NAME, filenameWithUUID);
            })
            .to(LOG_LINE_MESSAGE_PRODUCER_ROUTE);
    }
}
