package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.splitter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.splitter.LogGroupSplitterProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LoggGroupErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer.LogLineMessageProducer.LOG_LINE_MESSAGE_PRODUCER_ROUTE;

@Component
public class LogGroupSplitter extends LoggGroupErrorHandler {

    public static String LOG_GROUP_SPLITTER_ID = "log-group-splitter";
    public static String LOG_GROUP_SPLITTER_ROUTE = "direct:" + LOG_GROUP_SPLITTER_ID;

    @Override
    public void configure() {
        super.errorHandling();

        from(LOG_GROUP_SPLITTER_ROUTE)
                .routeId(LOG_GROUP_SPLITTER_ID)
                .log(LoggingLevel.INFO, "Splitting log file ${header.CamelFileName} into individual messages")
//                .throwException(new Exception("TESTING FAILURE BEFORE SPLITTING")) //TODO: remove after testing
                .split(body().tokenize("^\\<|\n\\<")).streaming()
                    .parallelProcessing()
                    .executorService("logLinePublishPool")
                    .stopOnException()
                    .shareUnitOfWork()
                    .bean(LogGroupSplitterProcessor.class, "prepareLogLineHeaders")
                    .to(LOG_LINE_MESSAGE_PRODUCER_ROUTE);
    }
}
