package no.nav.sikkerhetstjenesten.loggkamel.processor.enrichment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostgresLogEnrichment {

    //TODO: add validation on individual fields given what we can safely assume about them

    String logTime;
    String navIdent;
    String dbName;
    String auditType;
    String statementId;
    String substatementId;
    String pgAuditClass;
    String pgAuditCommand;
    String pgAuditObjectType;
    String pgAuditObjectName;
    String sqlStatement;
    String sqlParameter;
    String epost;
}
