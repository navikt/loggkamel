package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.splitter;

import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.splitter.NativeLogStreamSplitterProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogStreamErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer.NativeLogPacketProducer.NATIVE_LOG_PACKET_PRODUCER_ROUTE;

@Component
public class NativeLogStreamSplitter extends LogStreamErrorHandler {

    public static String NATIVE_LOG_STREAM_SPLITTER_ID = "native-log-stream-splitter";
    public static String NATIVE_LOG_STREAM_SPLITTER_ROUTE = "direct:" + NATIVE_LOG_STREAM_SPLITTER_ID;

    @Override
    public void configure() {
        super.errorHandling();

        from(NATIVE_LOG_STREAM_SPLITTER_ROUTE)
                .routeId(NATIVE_LOG_STREAM_SPLITTER_ID)
                .log(LoggingLevel.INFO, "Splitting log file ${header.CamelFileName} into bounded-size message lists")
                .split(method(NativeLogStreamSplitterProcessor.class, "groupIntoPackets"))
                    .streaming() //TODO: do we still want this parallellism? Would mean holding several in-progress packets in memory
//                    .parallelProcessing()
//                    .executorService("logPacketPublishPool")
//                    .shareUnitOfWork()
                    .bean(NativeLogStreamSplitterProcessor.class, "prepareLogLineHeaders")
                    .to(NATIVE_LOG_PACKET_PRODUCER_ROUTE);
    }
}
