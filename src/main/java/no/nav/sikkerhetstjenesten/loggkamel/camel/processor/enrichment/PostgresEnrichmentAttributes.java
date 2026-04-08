package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostgresEnrichmentAttributes {

    //TODO: add validation on individual fields given what we can safely assume about them

    private String logTime;
    private String navIdent;
    private String dbName;
    private String auditType;
    private String statementId;
    private String substatementId;
    private String pgAuditClass;
    private String pgCommand;
    private String pgObjectType;
    private String pgObjectName;
    private String sqlStatement;
    private String sqlParameters;
    private String epost;
}
