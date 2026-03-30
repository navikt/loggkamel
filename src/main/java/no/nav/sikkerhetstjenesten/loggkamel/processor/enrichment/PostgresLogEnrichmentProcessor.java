package no.nav.sikkerhetstjenesten.loggkamel.processor.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.processor.InvalidIndividualPostgresLog;
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
public class PostgresLogEnrichmentProcessor {
    private static final Logger log = LoggerFactory.getLogger(PostgresLogEnrichmentProcessor.class);

    static final String UNEXPECTED_LOG_PATTERN_MESSAGE = "Log failed to match expected pattern";
    static final String ENTRA_PROXY_ERROR_MESSAGE = "Error when fetching employee info";

    public static final String LOG_ENRICHMENT = "logEnrichment";

    private final EntraProxyService entraProxyService;
    private final LogRoutingAttributesEnricher logRoutingAttributesEnricher;

    @Autowired
    public PostgresLogEnrichmentProcessor(EntraProxyService entraProxyService, LogRoutingAttributesEnricher logRoutingAttributesEnricher) {
        this.logRoutingAttributesEnricher = logRoutingAttributesEnricher;
        this.entraProxyService = entraProxyService;
    }

    public void enrich(Exchange exchange) {
        Message msg = exchange.getMessage();
        String body = msg.getBody(String.class);

        if (body == null || body.isBlank()) {
            throw new InvalidIndividualPostgresLog("Audit log message is blank");
        }

        PostgresEnrichmentAttributes logEnrichment = extractEnrichmentFromLog(body);
        logEnrichment.setEpost(getAnsattEpost(logEnrichment.getNavIdent()));

        exchange.setVariable(LOG_ENRICHMENT, logEnrichment);

        LogRoutingAttributes routingAttributes = logRoutingAttributesEnricher.constructRoutingAttributesFromAuditClass(logEnrichment.getPgAuditClass());
        exchange.setProperty(LogRoutingAttributes.LOG_ROUTING_ATTRIBUTES, routingAttributes);

        msg.setBody(body);
    }

    private PostgresEnrichmentAttributes extractEnrichmentFromLog(String body) {
        String regex = "^(.*)\\(\\d+\\):v-oidc-(.*)-\\d+-.*@(.*?):.*(SESSION|OBJECT),(.*),(.*),(READ|WRITE|FUNCTION|ROLE|DDL|MISC|MISC_SET),(.*?),(.*?),(.*?),(\"|)?([\\s\\S]*)\\11,(\"|)?(.*)\\13";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(body);

        if (!matcher.find()) {
            //TODO: add identifier for the log line that doesn't leak PII here
            log.info(UNEXPECTED_LOG_PATTERN_MESSAGE);
            throw new InvalidIndividualPostgresLog(UNEXPECTED_LOG_PATTERN_MESSAGE);
        }

        return PostgresEnrichmentAttributes.builder()
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
        try {
            EntraProxyAnsatt entraProxyAnsatt = entraProxyService.getAnsattFromNavIdent(navIdent);

            // TODO: handle null or empty response here

            return entraProxyAnsatt.getEpost();
        } catch (Exception e) {
            // TODO: handle exceptions resulting from entra-proxy errors or service unavailable
            log.error(ENTRA_PROXY_ERROR_MESSAGE, e);
            throw new InvalidIndividualPostgresLog(ENTRA_PROXY_ERROR_MESSAGE, e);
        }
    }
}