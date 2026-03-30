package no.nav.sikkerhetstjenesten.loggkamel.processor.enrichment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogRoutingAttributes {

    public static String LOG_ROUTING_ATTRIBUTES = "logRoutingAttributes";

    boolean isRead;
    boolean isModification;

}
