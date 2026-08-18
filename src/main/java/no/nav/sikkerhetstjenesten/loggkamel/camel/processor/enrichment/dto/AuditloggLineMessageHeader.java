package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto;

import lombok.*;
import lombok.extern.jackson.Jacksonized;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.database.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggTaskDTO;

@Data
@Builder
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
public class AuditloggLineMessageHeader {

    public static final String TEKNOLOGI = "Teknologi";
    public static final String TEAM_GCP_PROJECT_ID = "TeamGcpProjectId";
    public static final String AUDITLOGG_TASK = "AuditloggTask";
    public static final String PLACE_IN_PACKET = "PlaceInPacket";

    @NonNull
    TeknologiEnum teknologi;
    @NonNull
    String teamGcpProjectId;
    @NonNull
    AuditloggTaskDTO auditloggTaskDTO;
    @NonNull
    Integer placeInPacket;
}
