package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.splitter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.SharedRouteErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment.LogLineEnricher.LOG_LINE_ENRICHER_ROUTE;
import static org.apache.camel.Exchange.FILE_NAME;

//TODO: given that this route does two things, either name something more generic or split into two routes
@Component
public class LogGroupSplitter extends SharedRouteErrorHandler {

    public static String LOG_GROUP_SPLITTER_ID = "log-group-splitter";
    public static String LOG_GROUP_SPLITTER_ROUTE = "direct:" + LOG_GROUP_SPLITTER_ID;

    @Override
    public void configure() {
        super.errorHandling();

        from(LOG_GROUP_SPLITTER_ROUTE)
            .routeId(LOG_GROUP_SPLITTER_ID)
            .log(LoggingLevel.INFO, "Decompressing and splitting log message ${header.CamelFileName}")
            .choice()
                .when(header(FILE_NAME).endsWith(".gz"))
                    .log(LoggingLevel.INFO, "Log file ${header.CamelFileName} is gzip compressed, attempting to decompress")
                    // if log file is compressed, decompress and remove the compression extension from the filename
                    .doTry()
                        .unmarshal().gzipDeflater()
                        .endDoTry()
                    .doCatch(IOException.class)
                        .log("Routing non-gzip or unreadable gzip input to invalid-messages channel: ${exception.message}, invalid filename: ${headers['CamelFileName']}")
                        .to(invalidMessageUri)
                        .stop()
                    .end()
                    .process(exchange -> {
                        String originalFileName = exchange.getIn().getHeader(FILE_NAME, String.class);
                        String newFileName = originalFileName.substring(0, originalFileName.length() - 3);

                        exchange.getIn().setHeader(FILE_NAME, newFileName);
                    })
                .end()
            .log(LoggingLevel.INFO, "Splitting log file ${header.CamelFileName} into individual messages")
            .split(body().tokenize("^\\<|\n\\<")).streaming()
            .process(exchange -> {
                //TODO: instead of modifying the filename here, toss a UUID into the header and use to generate filename only on local write or failure queues
                String originalFileName = exchange.getIn().getHeader(FILE_NAME, String.class);
                String fileExtension = originalFileName.contains(".") ? originalFileName.substring(originalFileName.lastIndexOf('.')) : "";
                String fileBeforeExtension = fileExtension.isEmpty() ? originalFileName : originalFileName.substring(0, originalFileName.lastIndexOf('.'));
                String newFileName = fileBeforeExtension + "." + UUID.randomUUID() + fileExtension;

                log.info("New filename being assigned: {}", newFileName);
                exchange.getIn().setHeader(FILE_NAME, newFileName);
            })
            .to(LOG_LINE_ENRICHER_ROUTE);
    }
}
