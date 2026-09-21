package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.producer;

import no.nav.boot.conditionals.ConditionalOnLocalOrTest;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.producer.LocalStandardizedLogLineProducerProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.observability.Metrics;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.LoggkamelHeaders.LOG_FILENAME;
import static org.apache.camel.Exchange.FILE_NAME;

@Component
@ConditionalOnLocalOrTest
public class LocalStandardizedLogLineProducer extends StandardizedLogLineProducer {

    private final LocalStandardizedLogLineProducerProcessor producerProcessor;

    @Value("${routing.logline.producer}")
    String producerUri;

    public LocalStandardizedLogLineProducer(LocalStandardizedLogLineProducerProcessor producerProcessor) {
        this.producerProcessor = producerProcessor;
    }

    @Override
    public void configure() {
        super.errorHandling(Metrics.Multiplicity.line);

        from(STANDARDIZED_LOG_LINE_PRODUCER_ROUTE)
                .routeId(STANDARDIZED_LOG_LINE_PRODUCER_ID)
                .log(LoggingLevel.INFO, "Producing log message ${header.LoggkamelFilename} line ${variable.PlaceInPacket} to local log")
                .process(producerProcessor::mapToJson)
                .process(producerProcessor::prepareLogLineHeaders)
                .setHeader(FILE_NAME, header(LOG_FILENAME))
                .to(producerUri);
    }
}
