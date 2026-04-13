package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogLineRoutingAttributes {

    public static String LOG_ROUTING_ATTRIBUTES = "LogLineRoutingAttributes";

    boolean isRead;
    boolean isModification;
}
