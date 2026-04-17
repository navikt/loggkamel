package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder(toBuilder = true)
@Jacksonized
@AllArgsConstructor
public class AuditloggLineMessage {

    String body;
    AuditloggLineMessageHeader header;
}
