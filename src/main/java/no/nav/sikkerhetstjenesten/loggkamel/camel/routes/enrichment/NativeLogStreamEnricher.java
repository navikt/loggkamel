package no.nav.sikkerhetstjenesten.loggkamel.camel.routes.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.NativeLogStreamEnrichmentProcessor;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.InputFileType;
import no.nav.sikkerhetstjenesten.loggkamel.camel.routes.error.RouteConfigurationIdResolver;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.camel.routes.filter.NativeLogStreamFilter.NATIVE_LOG_STREAM_FILTER_ROUTE;

@Component
public class NativeLogStreamEnricher extends RouteBuilder {

    private final NativeLogStreamEnrichmentProcessor enrichmentProcessor;
    private final RouteConfigurationIdResolver routeConfigurationIdResolver;

    public static final String NATIVE_LOG_STREAM_ENRICHER_ID = "native-log-stream-enricher";
    public static final String NATIVE_LOG_STREAM_ENRICHER_ROUTE = "direct:" + NATIVE_LOG_STREAM_ENRICHER_ID;

    public NativeLogStreamEnricher(
            NativeLogStreamEnrichmentProcessor enrichmentProcessor,
            RouteConfigurationIdResolver routeConfigurationIdResolver
    ) {
        this.enrichmentProcessor = enrichmentProcessor;
        this.routeConfigurationIdResolver = routeConfigurationIdResolver;
    }

    @Override
    public void configure() {
        from(NATIVE_LOG_STREAM_ENRICHER_ROUTE)
                .routeConfigurationId(routeConfigurationIdResolver.resolve(InputFileType.STREAM))
                .routeId(NATIVE_LOG_STREAM_ENRICHER_ID)
                .log(LoggingLevel.DEBUG, "Enriching stream-level attributes for ${header.LoggkamelFilename}")
                .process(enrichmentProcessor::enrich)
                .to(NATIVE_LOG_STREAM_FILTER_ROUTE);
    }
}
