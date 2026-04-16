package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.EntraProxyDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PostgresLogLineEnrichmentProcessor {
    private static final Logger log = LoggerFactory.getLogger(PostgresLogLineEnrichmentProcessor.class);

    static final String UNEXPECTED_LOG_PATTERN_MESSAGE = "Log failed to match expected pattern, cannot extract enrichment attributes";
    static final String ENTRA_PROXY_ERROR_MESSAGE = "Error when fetching ansatt information from entra-proxy";

    public static final String LOG_ENRICHMENT = "logLineEnrichment";
    static final String DB_AUDIT_ENTRY_REQUEST_TYPE = "dbAuditEntry";

    private final EntraProxyService entraProxyService;
    private final LogRoutingAttributesEnricher logRoutingAttributesEnricher;

    @Autowired
    public PostgresLogLineEnrichmentProcessor(EntraProxyService entraProxyService, LogRoutingAttributesEnricher logRoutingAttributesEnricher) {
        this.logRoutingAttributesEnricher = logRoutingAttributesEnricher;
        this.entraProxyService = entraProxyService;
    }

    public void enrich(Exchange exchange) {
        Message msg = exchange.getMessage();
        String body = msg.getBody(String.class);

        if (body == null || body.isBlank()) {
            throw new InvalidPostgresLogLineException("Audit log message is blank");
        }

        EnrichedLogMessage enrichedLogMessage = extractEnrichmentFromLog(body);
        enrichedLogMessage.setEpost(getAnsattEpost(enrichedLogMessage.getNavIdent()));
        exchange.setVariable(LOG_ENRICHMENT, enrichedLogMessage);

        LogLineRoutingAttributes routingAttributes = logRoutingAttributesEnricher.constructRoutingAttributesFromAuditClass(enrichedLogMessage.getPgAuditClass());
        exchange.setVariable(LogLineRoutingAttributes.LOG_ROUTING_ATTRIBUTES, routingAttributes);
    }

    private EnrichedLogMessage extractEnrichmentFromLog(String body) {
        String regex = "^(.*)\\(\\d+\\):v-oidc-(.*)-\\d+-.*@(.*?):.*(SESSION|OBJECT),(.*),(.*),(READ|WRITE|FUNCTION|ROLE|DDL|MISC|MISC_SET),(.*?),(.*?),(.*?),(\"|)?([\\s\\S]*)\\11,(\"|)?(.*)\\13";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(body);

        if (!matcher.find()) {
            log.info(UNEXPECTED_LOG_PATTERN_MESSAGE);
            throw new InvalidPostgresLogLineException(UNEXPECTED_LOG_PATTERN_MESSAGE);
        }

        return EnrichedLogMessage.builder()
                .originalMessage(body)
                .requestType(DB_AUDIT_ENTRY_REQUEST_TYPE)
                .logTime(matcher.group(1))
                .navIdent(matcher.group(2))
                .dbName(matcher.group(3))
                .auditType(matcher.group(4))
                .statementId(matcher.group(5))
                .substatementId(matcher.group(6))
                .pgAuditClass(matcher.group(7))
                .pgCommand(matcher.group(8))
                .pgObjectType(matcher.group(9))
                .pgObjectName(matcher.group(10))
                .sqlStatement(matcher.group(12))
                .sqlParameters(matcher.group(14))
                .build();
    }

    private String getAnsattEpost(String navIdent) {
        EntraProxyAnsatt entraProxyAnsatt;
        try {
            entraProxyAnsatt = entraProxyService.getAnsattFraNavIdent(navIdent);
        } catch (Exception e) {
            log.warn(ENTRA_PROXY_ERROR_MESSAGE, e);
            throw new EntraProxyDependencyException(ENTRA_PROXY_ERROR_MESSAGE, e);
        }

        if (entraProxyAnsatt == null || entraProxyAnsatt.getEpost() == null || entraProxyAnsatt.getEpost().isBlank()) {
            log.info("Entra-proxy returned empty response for navIdent {}, sending log to invalid messages", navIdent);
            throw new InvalidPostgresLogLineException("Entra-proxy returned empty response for navIdent " + navIdent);
        }

        return entraProxyAnsatt.getEpost();
    }
}