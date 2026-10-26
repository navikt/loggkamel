package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
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
    private String navIdent;
    private String tIdent;
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

    @JsonProperty("tIdent")
    public String getTIdent() {
        return tIdent;
    }

    @AssertTrue(message = "navIdent or tIdent must be non-null")
    @JsonIgnore
    public boolean isIdentityPresent() {
        return navIdent != null || tIdent != null;
    }

    // Used for routing by GCP log sinks
    private final String requestType = "dbAuditEntry";
}
