package no.nav.sikkerhetstjenesten.loggkamel.routes.consumer;

import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.processor.enrichment.LogRoutingAttributes;
import no.nav.sikkerhetstjenesten.loggkamel.routes.SharedRouteErrorHandler;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.sikkerhetstjenesten.loggkamel.processor.enrichment.LogRoutingAttributes.LOG_ROUTING_ATTRIBUTES;
import static no.nav.sikkerhetstjenesten.loggkamel.routes.enrichment.LogEnrichmentValues.TEKNOLOGI;
import static no.nav.sikkerhetstjenesten.loggkamel.routes.filter.LogGroupFilter.LOG_GROUP_FILTER_ROUTE;
import static org.apache.camel.Exchange.FILE_NAME;

@Component
public class PostgresLogConsumer extends SharedRouteErrorHandler {

    public static String POSTGRES_LOG_CONSUMER_ID = "postgres-log-consumer";

    @Value("${routing.postgres.consumer}")
    private String consumerUri;

    @Override
    public void configure() {
        super.errorHandling();

        //from("quartz://myGroup/myTestTimer?cron=*/10+*+*+*+*+?")

        from(consumerUri)
                .routeId(POSTGRES_LOG_CONSUMER_ID)
                .process(exchange -> exchange.setProperty(TEKNOLOGI, TeknologiEnum.POSTGRESQL))
                .process(exchange -> {
                    // If the file comes from a bucket instead of local storage, still populate the filename
                    if (exchange.getIn().getHeader(FILE_NAME, String.class) == null) {
                        exchange.getIn().setHeader(FILE_NAME, exchange.getIn().getHeader("CamelGoogleCloudStorageObjectName", String.class));
                    }
                })
                .log(LoggingLevel.INFO, "Processing postgres log message from ${header.CamelFileName}")
                .to(LOG_GROUP_FILTER_ROUTE);
    }
}
