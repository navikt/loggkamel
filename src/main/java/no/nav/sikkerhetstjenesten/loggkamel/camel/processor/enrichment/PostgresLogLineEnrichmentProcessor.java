package no.nav.sikkerhetstjenesten.loggkamel.camel.processor.enrichment;

import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.dependency.EntraProxyDependencyException;
import no.nav.sikkerhetstjenesten.loggkamel.client.EntraProxyAnsatt;
import no.nav.sikkerhetstjenesten.loggkamel.camel.exceptions.invalid.InvalidPostgresLogLineException;
import no.nav.sikkerhetstjenesten.loggkamel.observability.Metrics;
import no.nav.sikkerhetstjenesten.loggkamel.service.EntraProxyService;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PostgresLogLineEnrichmentProcessor {
    private static final Logger log = LoggerFactory.getLogger(PostgresLogLineEnrichmentProcessor.class);

    static final String UNEXPECTED_LOG_PATTERN_MESSAGE = "Log failed to match expected pattern, cannot extract enrichment attributes";
    static final String ENTRA_PROXY_ERROR_MESSAGE = "Error when fetching ansatt information from entra-proxy";
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS zzz", Locale.ENGLISH);

    private final EntraProxyService entraProxyService;
    private final LogLineOperationsEnricher logLineOperationsEnricher;
    private final Metrics metrics;

    @Autowired
    public PostgresLogLineEnrichmentProcessor(EntraProxyService entraProxyService, LogLineOperationsEnricher logLineOperationsEnricher, Metrics metrics) {
        this.logLineOperationsEnricher = logLineOperationsEnricher;
        this.entraProxyService = entraProxyService;
        this.metrics = metrics;
    }

    public void enrich(Exchange exchange) {
        String body = exchange.getMessage().getBody(AuditloggLineMessage.class).getBody();

        if (body == null || body.isBlank()) {
            throw new InvalidPostgresLogLineException("Audit log message is blank");
        }

        EnrichedAuditlogg enrichedAuditlogg;
        try {
            enrichedAuditlogg = extractEnrichmentFromLog(body);
        } catch (InvalidPostgresLogLineException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InvalidPostgresLogLineException("Failure converting values extracted from log line into EnrichedAuditlogg", e);
        }
        enrichedAuditlogg.setEpost(getAnsattEpost(enrichedAuditlogg.getNavIdent()));
        exchange.getMessage().setBody(enrichedAuditlogg);

        //TODO: once you have other teknologies and can see if this step is standardizable, pull this into the filtration step instead of
        // performing the operation preemptively here
        LogLineOperationTypes logLineOperationTypes = logLineOperationsEnricher.constructOperationTypesFromAuditClass(enrichedAuditlogg.getPgAuditClass());
        exchange.setVariable(LogLineOperationTypes.LOG_LINE_OPERATION_TYPES, logLineOperationTypes);
    }

    private EnrichedAuditlogg extractEnrichmentFromLog(String body) {
        String regex = "^(.*):\\d+\\.\\d+\\.\\d+\\.\\d+\\(\\d+\\):(.*)@(.*?):.*(SESSION|OBJECT),(.*),(.*),(READ|WRITE|FUNCTION|ROLE|DDL|MISC|MISC_SET),(.*?),(.*?),(.*?),(\"|)?([\\s\\S]*)\\11,(\"|)?(.*)\\13";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(body);

        if (!matcher.find()) {
            log.warn(UNEXPECTED_LOG_PATTERN_MESSAGE);
            throw new InvalidPostgresLogLineException(UNEXPECTED_LOG_PATTERN_MESSAGE);
        }

        ZonedDateTime logTime = ZonedDateTime.parse(matcher.group(1), DATE_TIME_FORMATTER);

        String userIdentity = matcher.group(2);
        // if the user identity is of the form "v-oidc-{navIdent}-something", extract the navIdent part. If it isn't of that form, then pass the full string to entra-proxy
        if (userIdentity.startsWith("v-oidc-")) {
            userIdentity = userIdentity.split("-")[2];
        }

        return EnrichedAuditlogg.builder()
                .originalMessage(body)
                .logTime(logTime)
                .navIdent(userIdentity)
                .dbName(matcher.group(3))
                .auditType(EnrichedAuditlogg.AuditType.valueOf(matcher.group(4)))
                .statementId(matcher.group(5))
                .substatementId(matcher.group(6))
                .pgAuditClass(EnrichedAuditlogg.AuditClass.valueOf(matcher.group(7)))
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
            log.info("Entra-proxy returned empty response for navIdent {}, not enriching with employee email", navIdent);
            metrics.incrementUnknownNavIdent();
            return null;
        }

        return entraProxyAnsatt.getEpost();
    }
}