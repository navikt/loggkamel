package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import no.nav.sikkerhetstjenesten.loggkamel.persistence.TeknologiEnum;
import no.nav.sikkerhetstjenesten.loggkamel.rest.dto.AuditloggArkivResponseDTO;

@Data
@Builder
@Jacksonized
@AllArgsConstructor
public class AuditloggLineMessageHeader {

    public static final String TEKNOLOGI = "Teknologi";
    public static final String TEAM_GCP_PROJECT_ID = "TeamGcpProjectId";
    public static final String AUDITLOGG_ARKIV = "AuditloggArkiv";

    TeknologiEnum teknologi;
    String teamGcpProjectId;
    AuditloggArkivResponseDTO auditloggArkivResponseDTO;
}
