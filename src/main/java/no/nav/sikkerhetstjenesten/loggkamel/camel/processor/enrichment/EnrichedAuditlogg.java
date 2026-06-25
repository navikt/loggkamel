package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnrichedAuditlogg {

    public enum AuditType {
        SESSION,
        OBJECT
    }

    public enum AuditClass {
        READ,
        WRITE,
        FUNCTION,
        ROLE,
        DDL,
        MISC,
        MISC_SET,
    }

    private String originalMessage;
    @NotNull
    private ZonedDateTime logTime;
    @NotNull
    private String navIdent;
    @NotNull
    private String dbName;
    @NotNull
    private AuditType auditType;
    private String statementId;
    private String substatementId;
    @NotNull
    private AuditClass pgAuditClass;
    private String pgCommand;
    private String pgObjectType;
    private String pgObjectName;
    @NotNull
    private String sqlStatement;
    private String sqlParameters;
    @Email
    private String epost;
    private static final String requestType = "dbAuditEntry";
}
