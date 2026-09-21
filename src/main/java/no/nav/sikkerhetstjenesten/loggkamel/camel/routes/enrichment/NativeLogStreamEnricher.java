package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.NativeLogStreamEnrichmentProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.LogStreamErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter.NativeLogStreamFilter.NATIVE_LOG_STREAM_FILTER_ROUTE;

@Component
public class NativeLogStreamEnricher extends LogStreamErrorHandler {

    private final NativeLogStreamEnrichmentProcessor enrichmentProcessor;

    public static final String NATIVE_LOG_STREAM_ENRICHER_ID = "native-log-stream-enricher";
    public static final String NATIVE_LOG_STREAM_ENRICHER_ROUTE = "direct:" + NATIVE_LOG_STREAM_ENRICHER_ID;

    public NativeLogStreamEnricher(NativeLogStreamEnrichmentProcessor enrichmentProcessor) {
        this.enrichmentProcessor = enrichmentProcessor;
    }

    @Override
    public void configure() {
        super.errorHandling();

        from(NATIVE_LOG_STREAM_ENRICHER_ROUTE)
                .routeId(NATIVE_LOG_STREAM_ENRICHER_ID)
                .log(LoggingLevel.DEBUG, "Enriching stream-level attributes for ${header.LoggkamelFilename}")
                .process(enrichmentProcessor::enrich)
                .process(blah -> {throw new InvalidLogLineException("scary exception bleerrggg");})
                .to(NATIVE_LOG_STREAM_FILTER_ROUTE);
    }
}
